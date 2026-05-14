package com.nbu.bank_system.controller;

import com.nbu.bank_system.dto.auth.CreateCorporateCustomerRequest;
import com.nbu.bank_system.dto.auth.CreateIndividualCustomerRequest;
import com.nbu.bank_system.dto.auth.OnboardingResponse;
import com.nbu.bank_system.service.CustomerOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Employee-facing controller за onboarding на индивидуални и корпоративни клиенти.
 * Приема валидирани request DTO-та и връща резултат от създаването заедно с информация за изпратената временна парола.
 */

@RestController
@RequestMapping("/api/employee/onboarding")
public class CustomerOnboardingController {

    private final CustomerOnboardingService customerOnboardingService;

    public CustomerOnboardingController(CustomerOnboardingService customerOnboardingService) {
        this.customerOnboardingService = customerOnboardingService;
    }

    @PostMapping("/individual")
    public ResponseEntity<OnboardingResponse> onboardIndividual(@Valid @RequestBody CreateIndividualCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerOnboardingService.onboardIndividual(request));
    }

    @PostMapping("/corporate")
    public ResponseEntity<OnboardingResponse> onboardCorporate(@Valid @RequestBody CreateCorporateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerOnboardingService.onboardCorporate(request));
    }
}

