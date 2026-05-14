package com.nbu.bank_system.controller;

import com.nbu.bank_system.dto.auth.AuthResponse;
import com.nbu.bank_system.dto.auth.ChangePasswordRequest;
import com.nbu.bank_system.dto.auth.ChangePasswordResponse;
import com.nbu.bank_system.dto.auth.LoginRequest;
import com.nbu.bank_system.dto.auth.PasswordResetRequest;
import com.nbu.bank_system.dto.auth.ResetPasswordRequest;
import com.nbu.bank_system.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller за authentication use cases.
 * Експонира login, first-login password change и password reset операции, като делегира бизнес логиката към AuthService.
 */

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ChangePasswordResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(new ChangePasswordResponse("If an account exists for this email, a reset link has been sent."));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ChangePasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new ChangePasswordResponse("Password reset successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(new ChangePasswordResponse("Password changed successfully"));
    }
}

