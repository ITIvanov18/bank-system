package com.nbu.bank_system.controller;

import com.nbu.bank_system.dto.auth.CreateCorporateCustomerRequest;
import com.nbu.bank_system.dto.auth.CreateIndividualCustomerRequest;
import com.nbu.bank_system.dto.auth.OnboardingResponse;
import com.nbu.bank_system.service.CustomerOnboardingService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Защитен onboarding controller за административно създаване на клиенти чрез secret header.
 * Използва се като bootstrap/демо вход към CustomerOnboardingService, когато няма интерактивен employee workflow.
 */

@RestController
@RequestMapping("/api/admin/secret/onboarding")
public class AdminOnboardingController {

    private static final String ADMIN_SECRET_HEADER = "X-Admin-Secret";

    private final CustomerOnboardingService customerOnboardingService;
    private final String adminSecret;

    public AdminOnboardingController(
            CustomerOnboardingService customerOnboardingService,
            @Value("${app.admin.secret}") String adminSecret
    ) {
        this.customerOnboardingService = customerOnboardingService;
        this.adminSecret = adminSecret;
    }

    @PostMapping("/individual")
    public ResponseEntity<?> onboardIndividual(
            @RequestHeader(value = ADMIN_SECRET_HEADER, required = false) String providedSecret,
            @Valid @RequestBody CreateIndividualCustomerRequest request
    ) {
        ResponseEntity<Map<String, String>> forbiddenResponse = validateSecret(providedSecret);
        if (forbiddenResponse != null) {
            return forbiddenResponse;
        }

        OnboardingResponse response = customerOnboardingService.onboardIndividual(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/corporate")
    public ResponseEntity<?> onboardCorporate(
            @RequestHeader(value = ADMIN_SECRET_HEADER, required = false) String providedSecret,
            @Valid @RequestBody CreateCorporateCustomerRequest request
    ) {
        ResponseEntity<Map<String, String>> forbiddenResponse = validateSecret(providedSecret);
        if (forbiddenResponse != null) {
            return forbiddenResponse;
        }

        OnboardingResponse response = customerOnboardingService.onboardCorporate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private ResponseEntity<Map<String, String>> validateSecret(String providedSecret) {
        if (providedSecret == null || !secureEquals(providedSecret, adminSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized admin secret"));
        }
        return null;
    }

    private boolean secureEquals(String left, String right) {
        // MessageDigest.isEqual избягва обикновено String сравнение и намалява timing leak риска при secret checks.
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}

