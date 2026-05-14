package com.nbu.bank_system.controller;

import com.nbu.bank_system.dto.loan.GrantLoanRequest;
import com.nbu.bank_system.dto.loan.LoanGrantResponse;
import com.nbu.bank_system.dto.loan.LoanReviewLogResponse;
import com.nbu.bank_system.dto.loan.LoanReviewRequest;
import com.nbu.bank_system.service.loan.LoanGrantingService;
import jakarta.validation.Valid;
import java.util.List;
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

/**
 * Employee-facing controller за обработка на кредити.
 * Покрива директно отпускане, преглед на pending заявления, approve/reject flow и история на решенията.
 */

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

    @GetMapping("/applications/pending")
    public ResponseEntity<List<LoanGrantResponse>> getPendingApplications() {
        return ResponseEntity.ok(loanGrantingService.getPendingApplications());
    }

    @PostMapping("/applications/{loanId}/approve")
    public ResponseEntity<LoanGrantResponse> approveApplication(
            @PathVariable Long loanId,
            Authentication authentication,
            @Valid @RequestBody(required = false) LoanReviewRequest request
    ) {
        String decisionNote = request != null ? request.decisionNote() : null;
        LoanGrantResponse response = loanGrantingService.approveApplication(
                loanId,
                authentication.getName(),
                decisionNote
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/applications/{loanId}/reject")
    public ResponseEntity<LoanGrantResponse> rejectApplication(
            @PathVariable Long loanId,
            Authentication authentication,
            @Valid @RequestBody(required = false) LoanReviewRequest request
    ) {
        String decisionNote = request != null ? request.decisionNote() : null;
        LoanGrantResponse response = loanGrantingService.rejectApplication(
                loanId,
                authentication.getName(),
                decisionNote
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/applications/history")
    public ResponseEntity<List<LoanReviewLogResponse>> getReviewHistory() {
        return ResponseEntity.ok(loanGrantingService.getReviewHistory());
    }
}
