package com.fundoonotes.fundoo_notes.service.impl;

import com.fundoonotes.fundoo_notes.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8086}")
    private String backendUrl;

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        String link = backendUrl + "/api/users/verify?token=" + token;

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

        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Fundoo Notes Account");
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("[MAIL ERROR] HTML verification email failed to {}. FROM={} Cause: {}", toEmail, fromEmail, e.getMessage(), e);
            try {
                sendEmail(toEmail,
                        "Verify Your Fundoo Notes Account",
                        "Hello,\n\nClick to verify your account:\n\n"
                                + link + "\n\nThis link expires in 24 hours.\n\n"
                                + "Regards,\nFundoo Notes Team");
            } catch (Exception ex) {
                log.error("[MAIL ERROR] Plain-text verification email also failed to {}: {}", toEmail, ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;

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

        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your Fundoo Notes Password");
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("[MAIL ERROR] HTML password reset email failed to {}. FROM={} Cause: {}", toEmail, fromEmail, e.getMessage(), e);
            try {
                sendEmail(toEmail,
                        "Reset Your Fundoo Notes Password",
                        "Hello,\n\nClick to reset your password:\n\n"
                                + link + "\n\nThis link expires in 24 hours.\n\n"
                                + "Regards,\nFundoo Notes Team");
            } catch (Exception ex) {
                log.error("[MAIL ERROR] Plain-text password reset email also failed to {}: {}", toEmail, ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void sendReminderEmail(String toEmail, String noteTitle) {
        sendReminderEmail(toEmail, noteTitle, "");
    }

    @Override
    public void sendReminderEmail(String toEmail, String noteTitle, String noteContent) {
        String title = (noteTitle != null && !noteTitle.trim().isEmpty()) ? noteTitle : "Untitled Note";
        String contentText = (noteContent != null && !noteContent.trim().isEmpty()) ? noteContent : "";
        String dashboardUrl = frontendUrl + "/dashboard";

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

        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reminder: " + title);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Reminder email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send HTML reminder email to {}: {}. Retrying with plain text.", toEmail, e.getMessage());
            try {
                sendEmail(toEmail,
                        "Reminder: " + title,
                        "Hello,\n\nThis is a reminder for your note:\n\n\"" + title + "\"\n"
                                + (contentText.isEmpty() ? "" : "\nContent:\n" + contentText + "\n")
                                + "\nPlease check your Fundoo Notes.\n\nRegards,\nFundoo Notes Team");
            } catch (Exception ex) {
                log.error("Failed to send reminder email to {}: {}", toEmail, ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void sendCollaborationEmail(String toEmail, String ownerEmail, String noteTitle, Long noteId) {
        String dashboardUrl = frontendUrl + "/dashboard?noteId=" + (noteId != null ? noteId : "");
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

        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Note shared with you - Fundoo Notes");
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Collaboration email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send HTML collaboration email to {}: {}. Retrying with plain text.", toEmail, e.getMessage());
            try {
                sendEmail(toEmail,
                        "Note shared with you - Fundoo Notes",
                        ownerEmail + " shared a note with you: \"" + title + "\"\n\nOpen note here:\n" + dashboardUrl);
            } catch (Exception ex) {
                log.error("Failed to send collaboration email to {}: {}", toEmail, ex.getMessage(), ex);
            }
        }
    }

    private void sendEmail(String to,
                           String subject,
                           String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}