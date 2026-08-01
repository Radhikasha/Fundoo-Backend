package com.fundoonotes.fundoo_notes.service.impl;

import com.fundoonotes.fundoo_notes.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:radhikasharma790670@gmail.com}}")
    private String fromEmail;

    @Value("${app.mail.api-key:${BREVO_API_KEY:${MAIL_PASSWORD:}}}")
    private String brevoApiKey;

    @Value("${app.frontend-url:https://fundoonotess.vercel.app}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8086}")
    private String backendUrl;

    private String getCleanFrontendUrl() {
        if (frontendUrl == null || frontendUrl.isBlank()) return "https://fundoonotess.vercel.app";
        String url = frontendUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.contains(".vercel.app") && url.contains("_")) {
            url = url.replace("_", "-");
        }
        return url;
    }

    private String getCleanBackendUrl() {
        if (backendUrl == null || backendUrl.isBlank()) return "http://localhost:8086";
        String url = backendUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Sends email via Brevo's HTTPS REST API (Port 443).
     * This bypasses Render Free Tier's outbound SMTP port blocking (ports 25/465/587).
     */
    private boolean sendViaBrevoHttpApi(String toEmail, String subject, String htmlContent) {
        if (brevoApiKey == null || brevoApiKey.isBlank() || brevoApiKey.contains("your-brevo") || brevoApiKey.contains("placeholder")) {
            log.info("Brevo API key not set or is default placeholder. Skipping HTTP API call.");
            return false;
        }

        try {
            log.info("Attempting to send email to {} via Brevo HTTP REST API (https://api.brevo.com/v3/smtp/email)...", toEmail);
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey.trim());
            headers.set("accept", "application/json");

            Map<String, Object> sender = new HashMap<>();
            sender.put("name", "Fundoo Notes");
            sender.put("email", fromEmail != null && !fromEmail.isBlank() ? fromEmail.trim() : "radhikasharma790670@gmail.com");

            Map<String, Object> toItem = new HashMap<>();
            toItem.put("email", toEmail.trim());

            Map<String, Object> body = new HashMap<>();
            body.put("sender", sender);
            body.put("to", Collections.singletonList(toItem));
            body.put("subject", subject);
            body.put("htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SUCCESS: Email sent to {} via Brevo HTTP REST API! Response: {}", toEmail, response.getBody());
                return true;
            } else {
                log.warn("Brevo HTTP API returned status {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Brevo HTTP REST API failed for {}: {}", toEmail, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        String link = getCleanBackendUrl() + "/api/users/verify?token=" + token;

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 580px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.05);\">"
                + "  <div style=\"padding: 30px; text-align: center; background-color: #ffffff;\">"
                + "    <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin: 0 auto 20px auto; width: 60px; height: 60px;\">"
                + "      <tr>"
                + "        <td align=\"center\" valign=\"middle\" style=\"width: 60px; height: 60px; background-color: #fbbc04; border-radius: 50%; text-align: center; vertical-align: middle; font-size: 26px; line-height: 60px; margin: 0; padding: 0;\">✉️</td>"
                + "      </tr>"
                + "    </table>"
                + "    <h2 style=\"font-size: 20px; color: #202124; margin-bottom: 15px;\">Verify Your Account</h2>"
                + "    <p style=\"font-size: 15px; color: #5f6368; margin-bottom: 25px;\">Thank you for signing up! Please click the button below to verify your Fundoo Notes account. This link will expire in 24 hours.</p>"
                + "    <a href=\"" + link + "\" style=\"display: inline-block; background-color: #fbbc04; color: #202124; text-decoration: none; font-weight: bold; font-size: 14px; padding: 12px 32px; border-radius: 6px; box-shadow: 0 1px 3px rgba(0,0,0,0.2);\">Verify Account</a>"
                + "  </div>"
                + "  <div style=\"background-color: #fef7e0; padding: 12px 20px; text-align: center; border-top: 1px solid #f1f3f4; font-size: 13px; color: #5f6368;\">"
                + "    <strong>Fundoo Notes</strong> &nbsp; Save your thoughts, wherever you are."
                + "  </div>"
                + "</div>";

        if (sendViaBrevoHttpApi(toEmail, "Verify Your Fundoo Notes Account", htmlContent)) {
            return;
        }

        sendSmtpFallback(toEmail, "Verify Your Fundoo Notes Account", htmlContent,
                "Hello,\n\nClick to verify your account:\n\n" + link + "\n\nThis link expires in 24 hours.\n\nRegards,\nFundoo Notes Team");
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = getCleanFrontendUrl() + "/reset-password?token=" + token;

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 580px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.05);\">"
                + "  <div style=\"padding: 30px; text-align: center; background-color: #ffffff;\">"
                + "    <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin: 0 auto 20px auto; width: 60px; height: 60px;\">"
                + "      <tr>"
                + "        <td align=\"center\" valign=\"middle\" style=\"width: 60px; height: 60px; background-color: #fbbc04; border-radius: 50%; text-align: center; vertical-align: middle; font-size: 26px; line-height: 60px; margin: 0; padding: 0;\">🔑</td>"
                + "      </tr>"
                + "    </table>"
                + "    <h2 style=\"font-size: 20px; color: #202124; margin-bottom: 15px;\">Reset Your Password</h2>"
                + "    <p style=\"font-size: 15px; color: #5f6368; margin-bottom: 25px;\">Click the button below to reset your Fundoo Notes password. This link will expire in 24 hours.</p>"
                + "    <a href=\"" + link + "\" style=\"display: inline-block; background-color: #fbbc04; color: #202124; text-decoration: none; font-weight: bold; font-size: 14px; padding: 12px 32px; border-radius: 6px; box-shadow: 0 1px 3px rgba(0,0,0,0.2);\">Reset Password</a>"
                + "    <p style=\"font-size: 13px; color: #70757a; margin-top: 25px; margin-bottom: 0;\">If you did not request a password reset, please ignore this email.</p>"
                + "  </div>"
                + "  <div style=\"background-color: #fef7e0; padding: 12px 20px; text-align: center; border-top: 1px solid #f1f3f4; font-size: 13px; color: #5f6368;\">"
                + "    <strong>Fundoo Notes</strong> &nbsp; Save your thoughts, wherever you are."
                + "  </div>"
                + "</div>";

        if (sendViaBrevoHttpApi(toEmail, "Reset Your Fundoo Notes Password", htmlContent)) {
            return;
        }

        sendSmtpFallback(toEmail, "Reset Your Fundoo Notes Password", htmlContent,
                "Hello,\n\nClick to reset your password:\n\n" + link + "\n\nThis link expires in 24 hours.\n\nRegards,\nFundoo Notes Team");
    }

    @Override
    public void sendReminderEmail(String toEmail, String noteTitle) {
        sendReminderEmail(toEmail, noteTitle, "");
    }

    @Override
    public void sendReminderEmail(String toEmail, String noteTitle, String noteContent) {
        String title = (noteTitle != null && !noteTitle.trim().isEmpty()) ? noteTitle : "Untitled Note";
        String contentText = (noteContent != null && !noteContent.trim().isEmpty()) ? noteContent : "";
        String dashboardUrl = getCleanFrontendUrl() + "/dashboard";

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 580px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.05);\">"
                + "  <div style=\"padding: 30px; text-align: center; background-color: #ffffff;\">"
                + "    <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin: 0 auto 20px auto; width: 60px; height: 60px;\">"
                + "      <tr>"
                + "        <td align=\"center\" valign=\"middle\" style=\"width: 60px; height: 60px; background-color: #fbbc04; border-radius: 50%; text-align: center; vertical-align: middle; font-size: 26px; line-height: 60px; margin: 0; padding: 0;\">⏰</td>"
                + "      </tr>"
                + "    </table>"
                + "    <p style=\"font-size: 15px; color: #5f6368; margin-bottom: 10px;\">Reminder for your note</p>"
                + "    <h2 style=\"font-size: 20px; color: #202124; margin-bottom: 15px; word-break: break-word;\">" + title + "</h2>"
                + (contentText.isEmpty() ? "" : "    <p style=\"font-size: 14px; color: #3c4043; background: #f8f9fa; padding: 15px; border-radius: 6px; text-align: left; white-space: pre-wrap; margin-bottom: 20px;\">" + contentText + "</p>")
                + "    <a href=\"" + dashboardUrl + "\" style=\"display: inline-block; background-color: #fbbc04; color: #202124; text-decoration: none; font-weight: bold; font-size: 14px; padding: 12px 32px; border-radius: 6px; box-shadow: 0 1px 3px rgba(0,0,0,0.2);\">Open in Fundoo Notes</a>"
                + "  </div>"
                + "  <div style=\"background-color: #fef7e0; padding: 12px 20px; text-align: center; border-top: 1px solid #f1f3f4; font-size: 13px; color: #5f6368;\">"
                + "    <strong>Fundoo Notes</strong> &nbsp; Save your thoughts, wherever you are."
                + "  </div>"
                + "</div>";

        if (sendViaBrevoHttpApi(toEmail, "Reminder: " + title, htmlContent)) {
            return;
        }

        sendSmtpFallback(toEmail, "Reminder: " + title, htmlContent,
                "Hello,\n\nThis is a reminder for your note:\n\n\"" + title + "\"\n"
                        + (contentText.isEmpty() ? "" : "\nContent:\n" + contentText + "\n")
                        + "\nPlease check your Fundoo Notes.\n\nRegards,\nFundoo Notes Team");
    }

    @Override
    public void sendCollaborationEmail(String toEmail, String ownerEmail, String noteTitle, Long noteId) {
        String dashboardUrl = getCleanFrontendUrl() + "/dashboard?noteId=" + (noteId != null ? noteId : "");
        String title = (noteTitle != null && !noteTitle.trim().isEmpty()) ? noteTitle : "Untitled Note";

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 580px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.05);\">"
                + "  <div style=\"padding: 30px; text-align: center; background-color: #ffffff;\">"
                + "    <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin: 0 auto 20px auto; width: 60px; height: 60px;\">"
                + "      <tr>"
                + "        <td align=\"center\" valign=\"middle\" style=\"width: 60px; height: 60px; background-color: #fbbc04; border-radius: 50%; text-align: center; vertical-align: middle; font-size: 26px; line-height: 60px; margin: 0; padding: 0;\">💡</td>"
                + "      </tr>"
                + "    </table>"
                + "    <p style=\"font-size: 15px; color: #5f6368; margin-bottom: 20px;\"><strong style=\"color: #202124;\">" + ownerEmail + "</strong> shared a note with you.</p>"
                + "    <h2 style=\"font-size: 20px; color: #202124; margin-bottom: 25px; word-break: break-word;\">" + title + "</h2>"
                + "    <a href=\"" + dashboardUrl + "\" style=\"display: inline-block; background-color: #fbbc04; color: #202124; text-decoration: none; font-weight: bold; font-size: 14px; padding: 12px 32px; border-radius: 6px; box-shadow: 0 1px 3px rgba(0,0,0,0.2);\">Open in Fundoo Notes</a>"
                + "  </div>"
                + "  <div style=\"background-color: #fef7e0; padding: 12px 20px; text-align: center; border-top: 1px solid #f1f3f4; font-size: 13px; color: #5f6368;\">"
                + "    <strong>Fundoo Notes</strong> &nbsp; Save your thoughts, wherever you are."
                + "  </div>"
                + "</div>";

        if (sendViaBrevoHttpApi(toEmail, "Note shared with you - Fundoo Notes", htmlContent)) {
            return;
        }

        sendSmtpFallback(toEmail, "Note shared with you - Fundoo Notes", htmlContent,
                ownerEmail + " shared a note with you: \"" + title + "\"\n\nOpen note here:\n" + dashboardUrl);
    }

    private void sendSmtpFallback(String toEmail, String subject, String htmlContent, String plainTextBody) {
        if (mailSender == null) {
            log.error("JavaMailSender is null and Brevo HTTP API failed. Email to {} could not be sent.", toEmail);
            return;
        }
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("SMTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("[SMTP MAIL ERROR] HTML email failed to {}. FROM={} Cause: {}", toEmail, fromEmail, e.getMessage(), e);
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(plainTextBody);
                mailSender.send(message);
                log.info("SMTP plain text email sent to {}", toEmail);
            } catch (Exception ex) {
                log.error("[SMTP MAIL ERROR] Plain-text email failed to {}: {}", toEmail, ex.getMessage(), ex);
            }
        }
    }
}