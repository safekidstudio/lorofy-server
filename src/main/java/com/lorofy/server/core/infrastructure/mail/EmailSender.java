package com.lorofy.server.core.infrastructure.mail;

import java.util.Map;

public interface EmailSender {
    // Send mail with template
    void sendWithTemplate(String to, String subject, String templateId, Map<String, Object> variables);

    // Send with HTML
    void sendHtml(String to, String subject, String htmlContent);
}
