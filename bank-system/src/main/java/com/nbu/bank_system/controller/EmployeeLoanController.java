package com.nbu.bank_system.controller;

import com.nbu.bank_system.dto.loan.GrantLoanRequest;
import com.nbu.bank_system.dto.loan.LoanGrantResponse;
import com.nbu.bank_system.service.loan.LoanGrantingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee/loans")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeLoanController {

    private final LoanGrantingService loanGrantingService;

    public EmployeeLoanController(LoanGrantingService loanGrantingService) {
        this.loanGrantingService = loanGrantingService;
    }

    @PostMapping("/grant")
    public ResponseEntity<LoanGrantResponse> grantLoan(@Valid @RequestBody GrantLoanRequest request) {
        LoanGrantResponse response = loanGrantingService.grantLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
