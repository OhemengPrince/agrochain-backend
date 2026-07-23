package com.agrochain.backend.controller;

import com.agrochain.backend.dto.EarningsDto;
import com.agrochain.backend.dto.MonthlyEarningsSummaryDto;
import com.agrochain.backend.dto.TransactionDto;
import com.agrochain.backend.dto.VerifyAccountResponse;
import com.agrochain.backend.dto.WithdrawRequest;
import com.agrochain.backend.dto.WithdrawalDto;
import com.agrochain.backend.model.EarningsTransactionStatus;
import com.agrochain.backend.model.TransactionType;
import com.agrochain.backend.service.EarningsService;
import com.agrochain.backend.service.UserService;
import com.agrochain.backend.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/earnings")
@RequiredArgsConstructor
public class EarningsController {

    private final EarningsService earningsService;
    private final WithdrawalService withdrawalService;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<EarningsDto> getMyEarnings(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return ResponseEntity.ok(earningsService.getEarnings(userId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionDto>> getTransactions(Authentication authentication,
                                                                  @RequestParam(required = false) TransactionType type,
                                                                  @RequestParam(required = false) EarningsTransactionStatus status,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        Long userId = currentUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(earningsService.getTransactionHistory(
                userId, type, status, startOfDay(startDate), endOfDay(endDate), pageable));
    }

    // Same filters as /transactions but unpaginated — the full matching set,
    // with every Transaction field, for reporting/export use cases.
    @GetMapping("/transactions/export")
    public ResponseEntity<List<TransactionDto>> exportTransactions(Authentication authentication,
                                                                     @RequestParam(required = false) TransactionType type,
                                                                     @RequestParam(required = false) EarningsTransactionStatus status,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = currentUserId(authentication);
        return ResponseEntity.ok(earningsService.exportTransactions(
                userId, type, status, startOfDay(startDate), endOfDay(endDate)));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<MonthlyEarningsSummaryDto>> getSummary(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return ResponseEntity.ok(earningsService.getMonthlySummary(userId));
    }

    @GetMapping("/withdrawals")
    public ResponseEntity<List<WithdrawalDto>> getWithdrawals(Authentication authentication) {
        return ResponseEntity.ok(withdrawalService.getWithdrawalHistory(authentication.getName()));
    }

    @GetMapping("/withdrawals/{id}")
    public ResponseEntity<WithdrawalDto> getWithdrawal(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(withdrawalService.getWithdrawalById(authentication.getName(), id));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawalDto> withdraw(Authentication authentication,
                                                    @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(withdrawalService.initiateWithdrawal(authentication.getName(), request));
    }

    @GetMapping("/verify-account")
    public ResponseEntity<VerifyAccountResponse> verifyAccount(@RequestParam String bankCode,
                                                                 @RequestParam String accountNumber) {
        return ResponseEntity.ok(withdrawalService.verifyAccountName(bankCode, accountNumber));
    }

    private Long currentUserId(Authentication authentication) {
        return userService.getCurrentUser(authentication.getName()).getId();
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX);
    }
}
