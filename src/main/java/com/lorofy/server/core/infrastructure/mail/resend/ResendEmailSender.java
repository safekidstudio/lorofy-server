package com.lorofy.server.core.infrastructure.mail.resend;

import com.lorofy.server.core.infrastructure.mail.EmailSender;
import com.resend.Resend;
import com.resend.services.emails.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResendEmailSender implements EmailSender {

    private final Resend resend;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Override
    public void sendWithTemplate(String to, String subject, String templateId, Map<String, Object> variables) {
        String sender = String.format("%s <%s>", fromName, fromAddress);

        Template template = Template.builder()
                .id(templateId)
                .variables(variables)
                .build();

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(sender)
                .to(to)
                .subject(subject)
                .template(template)
                .build();

        try {
            resend.emails().send(params);
            log.info("Email sent successfully using Resend Template to {}", to);
        } catch (Exception e) {
            log.error("Failed to send templated email to {}", to, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    @Override
    public void sendHtml(String to, String subject, String htmlContent) {
        String sender = String.format("%s <%s>", fromName, fromAddress);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(sender)
                .to(to)
                .subject(subject)
                .html(htmlContent)
                .build();

        try {
            resend.emails().send(params);
            log.info("HTML Email sent successfully using Resend SDK to {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}", to, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
