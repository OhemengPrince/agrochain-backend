package com.agrochain.backend.controller;

import com.agrochain.backend.dto.*;
import com.agrochain.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Please check your email for the OTP."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "New OTP sent to your email"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "OTP sent to your email."));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<Map<String, String>> verifyResetOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authService.verifyResetOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(Map.of("message", "OTP verified successfully."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    @PostMapping("/forgot-password-sms")
    public ResponseEntity<Map<String, String>> forgotPasswordSms(@Valid @RequestBody ForgotPasswordSmsRequest request) {
        authService.forgotPasswordSms(request);
        return ResponseEntity.ok(Map.of("message", "OTP sent to your phone."));
    }

    @PostMapping("/verify-reset-otp-sms")
    public ResponseEntity<Map<String, String>> verifyResetOtpSms(@Valid @RequestBody OtpVerifySmsRequest request) {
        authService.verifyResetOtpSms(request.getPhone(), request.getOtp());
        return ResponseEntity.ok(Map.of("message", "OTP verified successfully."));
    }

    @PostMapping("/reset-password-sms")
    public ResponseEntity<Map<String, String>> resetPasswordSms(@Valid @RequestBody ResetPasswordSmsRequest request) {
        authService.resetPasswordSms(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }
}
