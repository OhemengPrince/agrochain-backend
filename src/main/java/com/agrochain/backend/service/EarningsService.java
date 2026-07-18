package com.agrochain.backend.service;

import com.agrochain.backend.dto.EarningsDto;
import com.agrochain.backend.dto.TransactionDto;
import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.model.Earnings;
import com.agrochain.backend.model.EarningsTransactionStatus;
import com.agrochain.backend.model.Transaction;
import com.agrochain.backend.model.TransactionType;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.EarningsRepository;
import com.agrochain.backend.repository.TransactionRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * AgroChain commission structure: buyer pays listed price + 5%, seller
 * receives listed price - 10% (AgroChain keeps 15% total). pendingBalance and
 * availableBalance always hold the seller's NET share (already fee-deducted)
 * rather than the gross listed price — that's the only way the pending ->
 * available move in confirmEarning reconciles without a phantom balance gap.
 */
@Service
@RequiredArgsConstructor
public class EarningsService {

    private static final BigDecimal SELLER_NET_RATE = new BigDecimal("0.90");
    private static final BigDecimal SELLER_FEE_RATE = new BigDecimal("0.15");

    private final EarningsRepository earningsRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public EarningsDto getEarnings(Long userId) {
        Earnings earnings = getOrCreateEarnings(userId);
        return EarningsDto.builder()
                .availableBalance(earnings.getAvailableBalance())
                .pendingBalance(earnings.getPendingBalance())
                .totalEarned(earnings.getTotalEarned())
                .totalWithdrawn(earnings.getTotalWithdrawn())
                .totalAgrochainFee(earnings.getTotalAgrochainFee())
                .build();
    }

    public Page<TransactionDto> getTransactionHistory(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return transactionRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::toDto);
    }

    // grossAmount is the base listed price (before AgroChain's cut). Credits
    // the seller's pending_balance with their net share and records a PENDING
    // ledger row keyed by reference, to be released later via confirmEarning.
    @Transactional
    public void addPendingEarning(Long userId, TransactionType type, BigDecimal grossAmount, String description,
                                   String reference, String counterpartyName, Long relatedBookingId,
                                   Long relatedListingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal netAmount = grossAmount.multiply(SELLER_NET_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = grossAmount.multiply(SELLER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);

        Earnings earnings = getOrCreateEarnings(userId);
        earnings.setPendingBalance(earnings.getPendingBalance().add(netAmount));
        earningsRepository.save(earnings);

        Transaction transaction = Transaction.builder()
                .user(user)
                .type(type)
                .amount(grossAmount)
                .agrochainFee(fee)
                .netAmount(netAmount)
                .status(EarningsTransactionStatus.PENDING)
                .description(description)
                .reference(reference)
                .counterpartyName(counterpartyName)
                .relatedBookingId(relatedBookingId)
                .relatedListingId(relatedListingId)
                .build();
        transactionRepository.save(transaction);
    }

    // Moves a PENDING transaction's net amount from pending_balance to
    // available_balance and rolls it into the lifetime earnings/fee totals.
    @Transactional
    public void confirmEarning(String reference) {
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for reference " + reference));

        if (transaction.getStatus() != EarningsTransactionStatus.PENDING) {
            return;
        }

        Earnings earnings = getOrCreateEarnings(transaction.getUser().getId());
        earnings.setPendingBalance(earnings.getPendingBalance().subtract(transaction.getNetAmount()));
        earnings.setAvailableBalance(earnings.getAvailableBalance().add(transaction.getNetAmount()));
        earnings.setTotalEarned(earnings.getTotalEarned().add(transaction.getNetAmount()));
        earnings.setTotalAgrochainFee(earnings.getTotalAgrochainFee().add(transaction.getAgrochainFee()));
        earningsRepository.save(earnings);

        transaction.setStatus(EarningsTransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
    }

    // Reverses a PENDING transaction (e.g. booking cancelled before
    // completion) — removes it from pending_balance without ever touching
    // available_balance or the lifetime totals, since it was never confirmed.
    @Transactional
    public void reverseEarning(String reference) {
        transactionRepository.findByReference(reference).ifPresent(transaction -> {
            if (transaction.getStatus() != EarningsTransactionStatus.PENDING) {
                return;
            }
            Earnings earnings = getOrCreateEarnings(transaction.getUser().getId());
            earnings.setPendingBalance(earnings.getPendingBalance().subtract(transaction.getNetAmount()));
            earningsRepository.save(earnings);

            transaction.setStatus(EarningsTransactionStatus.CANCELLED);
            transactionRepository.save(transaction);
        });
    }

    // Used by withdrawals — moves money out of available_balance into
    // total_withdrawn. Throws if the balance can't cover it.
    @Transactional
    public void deductEarning(Long userId, BigDecimal amount) {
        Earnings earnings = getOrCreateEarnings(userId);
        if (earnings.getAvailableBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient available balance");
        }
        earnings.setAvailableBalance(earnings.getAvailableBalance().subtract(amount));
        earnings.setTotalWithdrawn(earnings.getTotalWithdrawn().add(amount));
        earningsRepository.save(earnings);
    }

    // Generic ledger entry for transactions that don't move pending/available
    // balances themselves (buyer payments, refunds, withdrawals) — those
    // balance changes are applied by the caller via deductEarning/etc.
    @Transactional
    public void recordTransaction(Long userId, TransactionType type, BigDecimal amount, BigDecimal fee,
                                   BigDecimal netAmount, EarningsTransactionStatus status,
                                   String description, String reference, String counterpartyName,
                                   Long relatedBookingId, Long relatedListingId, String paymentMethod,
                                   String paymentNumber, String bankName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Transaction transaction = Transaction.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .agrochainFee(fee)
                .netAmount(netAmount)
                .status(status)
                .description(description)
                .reference(reference)
                .counterpartyName(counterpartyName)
                .relatedBookingId(relatedBookingId)
                .relatedListingId(relatedListingId)
                .paymentMethod(paymentMethod)
                .paymentNumber(paymentNumber)
                .bankName(bankName)
                .build();
        transactionRepository.save(transaction);
    }

    Earnings getOrCreateEarnings(Long userId) {
        return earningsRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            return earningsRepository.save(Earnings.builder().user(user).build());
        });
    }

    private TransactionDto toDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .agrochainFee(transaction.getAgrochainFee())
                .netAmount(transaction.getNetAmount())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .reference(transaction.getReference())
                .counterpartyName(transaction.getCounterpartyName())
                .paymentMethod(transaction.getPaymentMethod())
                .paymentNumber(transaction.getPaymentNumber())
                .bankName(transaction.getBankName())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
