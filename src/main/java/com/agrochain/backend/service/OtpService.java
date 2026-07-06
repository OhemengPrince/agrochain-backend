package com.agrochain.backend.service;

import com.agrochain.backend.model.OtpChannel;
import com.agrochain.backend.model.OtpPurpose;
import com.agrochain.backend.model.OtpVerification;
import com.agrochain.backend.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_MINUTES = 5;

    private final OtpVerificationRepository otpVerificationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {
        int otp = secureRandom.nextInt(1_000_000);
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }

    public void saveOtp(String email, String otp, OtpPurpose purpose, OtpChannel channel) {
        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otpCode(otp)
                .purpose(purpose)
                .channel(channel)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES))
                .isUsed(false)
                .build();
        otpVerificationRepository.save(otpVerification);
    }

    public boolean isOtpValid(String email, String otp, OtpPurpose purpose) {
        return findValidOtp(email, otp, purpose).isPresent();
    }

    public void invalidateExistingOtps(String email, OtpPurpose purpose) {
        List<OtpVerification> existing = otpVerificationRepository.findByEmailAndPurposeAndIsUsedFalse(email, purpose);
        existing.forEach(otpVerification -> otpVerification.setUsed(true));
        otpVerificationRepository.saveAll(existing);
    }

    public void deleteAllForEmail(String email) {
        otpVerificationRepository.deleteByEmail(email);
    }

    public boolean verifyOtp(String email, String otp, OtpPurpose purpose) {
        Optional<OtpVerification> otpVerificationOpt = findValidOtp(email, otp, purpose);

        if (otpVerificationOpt.isEmpty()) {
            return false;
        }

        OtpVerification otpVerification = otpVerificationOpt.get();
        otpVerification.setUsed(true);
        otpVerificationRepository.save(otpVerification);
        return true;
    }

    private Optional<OtpVerification> findValidOtp(String email, String otp, OtpPurpose purpose) {
        return otpVerificationRepository.findByEmailAndOtpCodeAndIsUsedFalse(email, otp).stream()
                .filter(candidate -> candidate.getPurpose() == purpose)
                .filter(candidate -> candidate.getExpiresAt().isAfter(LocalDateTime.now()))
                .findFirst();
    }
}
