package com.nbu.bank_system.controller;

import com.nbu.bank_system.dto.loan.LoanGrantResponse;
import com.nbu.bank_system.dto.loan.SubmitLoanApplicationRequest;
import com.nbu.bank_system.service.loan.LoanGrantingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/loans")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerLoanController {

    private final LoanGrantingService loanGrantingService;

    public CustomerLoanController(LoanGrantingService loanGrantingService) {
        this.loanGrantingService = loanGrantingService;
    }

    @PostMapping("/applications")
    public ResponseEntity<LoanGrantResponse> submitApplication(
            Authentication authentication,
            @Valid @RequestBody SubmitLoanApplicationRequest request
    ) {
        LoanGrantResponse response = loanGrantingService.submitLoanApplication(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
