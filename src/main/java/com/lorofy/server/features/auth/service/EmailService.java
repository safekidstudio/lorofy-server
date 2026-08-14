package com.lorofy.server.features.auth.service;

import com.lorofy.server.core.infrastructure.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailSender emailSender; // Inject interface trung lập

    @Value("${app.mail.templates.otp}")
    private String otpTemplateId;

    public void sendOtpEmail(String toEmail, String otpCode) {
        emailSender.sendWithTemplate(
                toEmail,
                "Verify Your Email Address",
                otpTemplateId,
                Map.of("OTP_CODE", otpCode));
    }
}
