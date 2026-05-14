package com.nbu.bank_system.controller;

import com.nbu.bank_system.dto.loan.CustomerLoanApplicationStatusResponse;
import com.nbu.bank_system.dto.loan.LoanGrantResponse;
import com.nbu.bank_system.dto.loan.InstallmentPaymentLogResponse;
import com.nbu.bank_system.dto.loan.SubmitLoanApplicationRequest;
import com.nbu.bank_system.service.loan.LoanGrantingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer-facing controller за кредитни заявления, активни кредити и плащания.
 * Пази REST contract-а отделен от loan service логиката и връща DTO-та, пригодени за клиентския dashboard.
 */

@RestController
@RequestMapping("/api/customer/loans")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerLoanController {

    private final LoanGrantingService loanGrantingService;

    public CustomerLoanController(LoanGrantingService loanGrantingService) {
        this.loanGrantingService = loanGrantingService;
    }

    @PostMapping("/applications")
    public ResponseEntity<CustomerLoanApplicationStatusResponse> submitApplication(
            Authentication authentication,
            @Valid @RequestBody SubmitLoanApplicationRequest request
    ) {
        loanGrantingService.submitLoanApplication(authentication.getName(), request);
        CustomerLoanApplicationStatusResponse customerResponse = loanGrantingService
                .getLatestCustomerLoanApplication(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Loan application status could not be loaded."));
        return ResponseEntity.status(HttpStatus.CREATED).body(customerResponse);
    }

    @GetMapping("/applications/latest")
    public ResponseEntity<CustomerLoanApplicationStatusResponse> getLatestApplication(Authentication authentication) {
        return loanGrantingService.getLatestCustomerLoanApplication(authentication.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    @GetMapping("")
    public ResponseEntity<List<LoanGrantResponse>> getAllCustomerLoans(Authentication authentication) {
        return ResponseEntity.ok(loanGrantingService.getAllCustomerLoans(authentication.getName()));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<InstallmentPaymentLogResponse>> getCustomerPaymentLogs(Authentication authentication) {
        return ResponseEntity.ok(loanGrantingService.getCustomerPaymentLogs(authentication.getName()));
    }

    @PostMapping("/{loanId}/repay")
    public ResponseEntity<com.nbu.bank_system.dto.loan.LoanGrantResponse> repayInstallment(
            Authentication authentication,
            @PathVariable Long loanId
    ) {
        return ResponseEntity.ok(loanGrantingService.repayInstallment(loanId, authentication.getName()));
    }
}
