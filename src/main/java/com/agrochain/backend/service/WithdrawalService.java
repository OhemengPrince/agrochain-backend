package com.agrochain.backend.service;

import com.agrochain.backend.dto.PaystackRecipientResponse;
import com.agrochain.backend.dto.PaystackResolveResponse;
import com.agrochain.backend.dto.PaystackTransferResponse;
import com.agrochain.backend.dto.VerifyAccountResponse;
import com.agrochain.backend.dto.WithdrawRequest;
import com.agrochain.backend.dto.WithdrawalDto;
import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.model.EarningsTransactionStatus;
import com.agrochain.backend.model.MomoNetwork;
import com.agrochain.backend.model.TransactionType;
import com.agrochain.backend.model.User;
import com.agrochain.backend.model.Withdrawal;
import com.agrochain.backend.model.WithdrawalMethod;
import com.agrochain.backend.model.WithdrawalStatus;
import com.agrochain.backend.repository.UserRepository;
import com.agrochain.backend.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private static final String PAYSTACK_RECIPIENT_URL = "https://api.paystack.co/transferrecipient";
    private static final String PAYSTACK_TRANSFER_URL = "https://api.paystack.co/transfer";
    private static final String PAYSTACK_RESOLVE_URL = "https://api.paystack.co/bank/resolve";
    private static final BigDecimal MINIMUM_WITHDRAWAL = new BigDecimal("10.00");

    private final WithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;
    private final EarningsService earningsService;
    private final RestTemplate restTemplate;

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    public WithdrawalDto initiateWithdrawal(String userEmail, WithdrawRequest request) {
        User user = getUserOrThrow(userEmail);
        BigDecimal available = earningsService.getEarnings(user.getId()).getAvailableBalance();

        if (request.getAmount().compareTo(MINIMUM_WITHDRAWAL) < 0) {
            throw new BadRequestException("Minimum withdrawal is GHS 10.00");
        }
        if (available.compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient available balance");
        }

        Withdrawal withdrawal = request.getMethod() == WithdrawalMethod.MOMO
                ? buildMomoWithdrawal(user, request)
                : buildBankWithdrawal(user, request);

        try {
            String recipientCode = createTransferRecipient(request, user);
            PaystackTransferResponse transferResponse = initiateTransfer(recipientCode, request.getAmount(),
                    "AgroChain withdrawal for " + user.getFullName());

            if (transferResponse == null || !Boolean.TRUE.equals(transferResponse.getStatus())) {
                String message = transferResponse != null ? transferResponse.getMessage() : "No response from Paystack";
                withdrawal.setStatus(WithdrawalStatus.FAILED);
                withdrawalRepository.save(withdrawal);
                throw new BadRequestException("Withdrawal failed: " + message);
            }

            withdrawal.setPaystackReference(transferResponse.getData().getReference());
            withdrawal.setStatus(WithdrawalStatus.PENDING);
            Withdrawal saved = withdrawalRepository.save(withdrawal);

            earningsService.deductEarning(user.getId(), request.getAmount());

            String paymentNumber = request.getMethod() == WithdrawalMethod.MOMO
                    ? request.getPhoneNumber() : request.getAccountNumber();
            earningsService.recordTransaction(user.getId(), TransactionType.WITHDRAWAL, request.getAmount(),
                    BigDecimal.ZERO, request.getAmount(), EarningsTransactionStatus.PENDING,
                    "Withdrawal via " + request.getMethod(), "WD-" + saved.getId(), user.getFullName(),
                    null, request.getMethod().name(), paymentNumber, request.getBankName());

            return toDto(saved);
        } catch (RestClientException e) {
            log.error("Paystack transfer request failed: {}", e.getMessage());
            withdrawal.setStatus(WithdrawalStatus.FAILED);
            withdrawalRepository.save(withdrawal);
            throw new BadRequestException("Withdrawal failed: " + e.getMessage());
        }
    }

    public VerifyAccountResponse verifyAccountName(String bankCode, String accountNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);

        String url = PAYSTACK_RESOLVE_URL + "?account_number=" + accountNumber + "&bank_code=" + bankCode;

        PaystackResolveResponse response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                    PaystackResolveResponse.class).getBody();
        } catch (RestClientException e) {
            log.error("Paystack account resolve failed: {}", e.getMessage());
            return VerifyAccountResponse.builder().verified(false).accountNumber(accountNumber).build();
        }

        if (response == null || !Boolean.TRUE.equals(response.getStatus()) || response.getData() == null) {
            return VerifyAccountResponse.builder().verified(false).accountNumber(accountNumber).build();
        }

        return VerifyAccountResponse.builder()
                .verified(true)
                .accountNumber(response.getData().getAccountNumber())
                .accountName(response.getData().getAccountName())
                .build();
    }

    public List<WithdrawalDto> getWithdrawalHistory(String userEmail) {
        User user = getUserOrThrow(userEmail);
        return withdrawalRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toDto)
                .toList();
    }

    private String createTransferRecipient(WithdrawRequest request, User user) {
        Map<String, Object> body = new HashMap<>();
        if (request.getMethod() == WithdrawalMethod.MOMO) {
            body.put("type", "mobile_money");
            body.put("name", user.getFullName());
            body.put("account_number", request.getPhoneNumber());
            body.put("bank_code", toPaystackMomoCode(request.getNetwork()));
        } else {
            body.put("type", "nuban");
            body.put("name", request.getAccountName() != null ? request.getAccountName() : user.getFullName());
            body.put("account_number", request.getAccountNumber());
            body.put("bank_code", request.getBankCode());
        }
        body.put("currency", "GHS");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        PaystackRecipientResponse response = restTemplate.postForObject(PAYSTACK_RECIPIENT_URL,
                new HttpEntity<>(body, headers), PaystackRecipientResponse.class);

        if (response == null || !Boolean.TRUE.equals(response.getStatus()) || response.getData() == null) {
            String message = response != null ? response.getMessage() : "No response from Paystack";
            throw new BadRequestException("Could not create transfer recipient: " + message);
        }

        return response.getData().getRecipientCode();
    }

    private PaystackTransferResponse initiateTransfer(String recipientCode, BigDecimal amount, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("source", "balance");
        body.put("amount", amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact());
        body.put("recipient", recipientCode);
        body.put("reason", reason);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.postForObject(PAYSTACK_TRANSFER_URL, new HttpEntity<>(body, headers),
                PaystackTransferResponse.class);
    }

    private Withdrawal buildMomoWithdrawal(User user, WithdrawRequest request) {
        return Withdrawal.builder()
                .user(user)
                .amount(request.getAmount())
                .method(WithdrawalMethod.MOMO)
                .network(request.getNetwork() != null ? request.getNetwork().name() : null)
                .phoneNumber(request.getPhoneNumber())
                .status(WithdrawalStatus.PENDING)
                .build();
    }

    private Withdrawal buildBankWithdrawal(User user, WithdrawRequest request) {
        return Withdrawal.builder()
                .user(user)
                .amount(request.getAmount())
                .method(WithdrawalMethod.BANK)
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .accountName(request.getAccountName())
                .status(WithdrawalStatus.PENDING)
                .build();
    }

    private String toPaystackMomoCode(MomoNetwork network) {
        if (network == null) {
            throw new BadRequestException("Network is required for MoMo withdrawal");
        }
        return switch (network) {
            case MTN -> "MTN";
            case VODAFONE -> "VOD";
            case AIRTELTIGO -> "ATL";
        };
    }

    private WithdrawalDto toDto(Withdrawal withdrawal) {
        return WithdrawalDto.builder()
                .id(withdrawal.getId())
                .amount(withdrawal.getAmount())
                .method(withdrawal.getMethod())
                .network(withdrawal.getNetwork())
                .phoneNumber(withdrawal.getPhoneNumber())
                .bankName(withdrawal.getBankName())
                .accountNumber(withdrawal.getAccountNumber())
                .accountName(withdrawal.getAccountName())
                .status(withdrawal.getStatus())
                .createdAt(withdrawal.getCreatedAt())
                .build();
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
