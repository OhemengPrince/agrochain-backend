package com.agrochain.backend.repository;

import com.agrochain.backend.model.OtpChannel;
import com.agrochain.backend.model.OtpPurpose;
import com.agrochain.backend.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByEmailAndOtpCodeAndIsUsedFalse(String email, String otpCode);

    List<OtpVerification> findByEmailAndPurposeAndIsUsedFalse(String email, OtpPurpose purpose);

    Optional<OtpVerification> findByEmailAndOtpCodeAndPurposeAndChannelAndIsUsedFalse(
            String email, String otpCode, OtpPurpose purpose, OtpChannel channel);

    void deleteByEmail(String email);
}
