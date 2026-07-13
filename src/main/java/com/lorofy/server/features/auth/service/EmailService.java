package com.lorofy.server.features.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email Address");

            // Template
            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 5px;">
                        <h2 style="color: #4F46E5; text-align: center;">Xác thực tài khoản Lorofy</h2>
                        <p>Chào bạn,</p>
                        <p>Bạn đang đăng ký tài khoản tại Lorofy. Dưới đây là mã xác thực OTP của bạn:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #111827; background-color: #F3F4F6; padding: 10px 20px; border-radius: 5px; border: 1px dashed #D1D5DB;">
                                %s
                            </span>
                        </div>
                        <p style="color: #6B7280; font-size: 14px;">Mã OTP này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                        <hr style="border: 0; border-top: 1px solid #eee; margin: 20px 0;"/>
                        <p style="color: #9CA3AF; font-size: 12px; text-align: center;">Đây là email tự động, vui lòng không phản hồi email này.</p>
                    </div>
                    """
                    .formatted(otpCode);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email OTP sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Could not send verification email. Please try again.");
        }
    }
}
