package com.agrochain.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
            helper.setFrom("princeohemeng688@gmail.com", "AgroChain");
            helper.setTo(toEmail);
            helper.setSubject("AgroChain - Your OTP Code");
            helper.setText("Your OTP code for " + purpose + " is: " + otp
                    + "\n\nThis code will expire in 5 minutes.\n\nIf you did not request this, please ignore this email.");
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }
}
