package com.nbu.bank_system.controller;

import com.nbu.bank_system.dto.account.AccountOpeningResponse;
import com.nbu.bank_system.dto.account.AccountStatusResponse;
import com.nbu.bank_system.service.CustomerAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-facing controller за банкови сметки.
 * Работи само за CUSTOMER роля и използва authenticated principal email-а като граница между клиента и service слоя.
 */

@RestController
@RequestMapping("/api/customer/accounts")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAccountController {

    private final CustomerAccountService customerAccountService;

    public CustomerAccountController(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    @GetMapping("/status")
    public ResponseEntity<AccountStatusResponse> getAccountStatus(Authentication authentication) {
        AccountStatusResponse response = customerAccountService.getAccountStatus(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/open")
    public ResponseEntity<AccountOpeningResponse> openAccount(Authentication authentication) {
        AccountOpeningResponse response = customerAccountService.openAccountForCustomer(authentication.getName());
        HttpStatus status = response.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}

