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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @Transactional
    public void register(RegisterRequest request) {
        log.info("Registration attempt: email={}, role={}", request.getEmail(), request.getRole());

        User existing = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existing != null && existing.isVerified()) {
            log.warn("Registration rejected, email already verified: {}", request.getEmail());
            throw new BadRequestException("Email already registered");
        }

        if (existing != null) {
            log.info("Unverified account found for {}, deleting old record and OTPs before re-registering", request.getEmail());
            userRepository.delete(existing);
            userRepository.flush();
            otpService.deleteAllForEmail(request.getEmail());
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
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration race detected, email was registered by a concurrent request: {}", request.getEmail());
            throw new BadRequestException("Email already registered");
        }
        log.info("User created: id={}, email={}", user.getId(), user.getEmail());

        String otp = otpService.generateOtp();
        otpService.saveOtp(user.getEmail(), otp, OtpPurpose.REGISTRATION, OtpChannel.EMAIL);
        emailService.sendOtpEmail(user.getEmail(), otp, "registration");
        log.info("Registration OTP sent to {}", user.getEmail());
    }

    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (user.isVerified()) {
            throw new BadRequestException("Account already verified");
        }

        otpService.invalidateExistingOtps(email, OtpPurpose.REGISTRATION);

        String otp = otpService.generateOtp();
        otpService.saveOtp(email, otp, OtpPurpose.REGISTRATION, OtpChannel.EMAIL);
        emailService.sendOtpEmail(email, otp, "registration");
        log.info("OTP resent to {}", email);
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
            throw new UnauthorizedException("Account not verified. Please check your email for your OTP or request a new one.");
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .user(UserMapper.toDto(user))
                .build();
    }

    // Existing account -> issue a token like a normal login (Google's
    // verification is at least as strong as our own OTP flow, so an
    // account that never finished email OTP verification is fine to trust
    // here too). New email -> hand the verified profile fields back so the
    // frontend can collect role/region/etc. and call googleRegister.
    @Transactional
    public GoogleLoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleTokenInfoResponse googleUser = verifyGoogleIdToken(request.getIdToken());

        User user = userRepository.findByEmail(googleUser.getEmail()).orElse(null);
        if (user == null) {
            return GoogleLoginResponse.builder()
                    .newUser(true)
                    .email(googleUser.getEmail())
                    .fullName(googleUser.getName())
                    .profilePhotoUrl(googleUser.getPicture())
                    .build();
        }

        if (!user.isVerified()) {
            user.setVerified(true);
        }
        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved);
        return GoogleLoginResponse.builder()
                .newUser(false)
                .token(token)
                .user(UserMapper.toDto(saved))
                .build();
    }

    @Transactional
    public AuthResponse googleRegister(GoogleRegisterRequest request) {
        GoogleTokenInfoResponse googleUser = verifyGoogleIdToken(request.getIdToken());

        if (userRepository.findByEmail(googleUser.getEmail()).isPresent()) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .fullName(googleUser.getName())
                .email(googleUser.getEmail())
                .phoneNumber(request.getPhoneNumber())
                // Google users never set a password — a random, never-shared
                // hash keeps the NOT NULL column satisfied while making
                // password-based login impossible for this account.
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(request.getRole())
                .region(request.getRegion())
                .district(request.getDistrict())
                .profilePhotoUrl(googleUser.getPicture())
                .isVerified(true)
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.warn("Google registration race detected, email was registered by a concurrent request: {}",
                    googleUser.getEmail());
            throw new BadRequestException("Email already registered");
        }
        log.info("User created via Google Sign-In: id={}, email={}", user.getId(), user.getEmail());

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .user(UserMapper.toDto(user))
                .build();
    }

    // Verifies the ID token against Google's tokeninfo endpoint: the "aud"
    // claim must match our OAuth client id (otherwise a token minted for a
    // different app could be replayed here) and "email_verified" must be
    // true. Never trust email/name/picture from the request body — only
    // from this verified response.
    private GoogleTokenInfoResponse verifyGoogleIdToken(String idToken) {
        GoogleTokenInfoResponse response;
        try {
            response = restTemplate.getForObject(
                    GOOGLE_TOKENINFO_URL + "?id_token={idToken}", GoogleTokenInfoResponse.class, idToken);
        } catch (RestClientException e) {
            log.warn("Google token verification request failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Google token");
        }

        if (response == null || response.getError() != null) {
            String reason = response != null ? response.getErrorDescription() : "no response";
            log.warn("Google token verification rejected: {}", reason);
            throw new UnauthorizedException("Invalid Google token");
        }
        if (googleClientId == null || googleClientId.isBlank() || !googleClientId.equals(response.getAud())) {
            log.warn("Google token aud did not match configured client id");
            throw new UnauthorizedException("Invalid Google token");
        }
        if (!"true".equals(response.getEmailVerified())) {
            throw new UnauthorizedException("Google account email is not verified");
        }
        if (response.getEmail() == null || response.getEmail().isBlank()) {
            throw new UnauthorizedException("Invalid Google token");
        }
        return response;
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
