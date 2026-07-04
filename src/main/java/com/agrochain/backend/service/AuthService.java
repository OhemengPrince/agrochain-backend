package com.agrochain.backend.service;

import com.agrochain.backend.dto.*;
import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.OtpChannel;
import com.agrochain.backend.model.OtpPurpose;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .region(request.getRegion())
                .district(request.getDistrict())
                .isVerified(false)
                .build();
        userRepository.save(user);

        String otp = otpService.generateOtp();
        otpService.saveOtp(user.getEmail(), otp, OtpPurpose.REGISTRATION, OtpChannel.EMAIL);
        emailService.sendOtpEmail(user.getEmail(), otp, "registration");
    }

    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpPurpose.REGISTRATION);
        if (!valid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setVerified(true);
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .user(UserMapper.toDto(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in");
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .user(UserMapper.toDto(user))
                .build();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otp = otpService.generateOtp();
        otpService.saveOtp(user.getEmail(), otp, OtpPurpose.PASSWORD_RESET, OtpChannel.EMAIL);
        emailService.sendOtpEmail(user.getEmail(), otp, "password reset");
    }

    public void verifyResetOtp(String email, String otp) {
        boolean valid = otpService.isOtpValid(email, otp, OtpPurpose.PASSWORD_RESET);
        if (!valid) {
            throw new BadRequestException("Invalid or expired OTP");
        }
    }

    public void resetPassword(ResetPasswordRequest request) {
        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpPurpose.PASSWORD_RESET);
        if (!valid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public void forgotPasswordSms(ForgotPasswordSmsRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otp = otpService.generateOtp();
        otpService.saveOtp(user.getPhoneNumber(), otp, OtpPurpose.PASSWORD_RESET, OtpChannel.SMS);
        log.info("[MOCK SMS] Sending OTP {} to phone {}", otp, user.getPhoneNumber());
    }

    public void verifyResetOtpSms(String phone, String otp) {
        boolean valid = otpService.isOtpValid(phone, otp, OtpPurpose.PASSWORD_RESET);
        if (!valid) {
            throw new BadRequestException("Invalid or expired OTP");
        }
    }

    public void resetPasswordSms(ResetPasswordSmsRequest request) {
        boolean valid = otpService.verifyOtp(request.getPhone(), request.getOtp(), OtpPurpose.PASSWORD_RESET);
        if (!valid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        User user = userRepository.findByPhoneNumber(request.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
