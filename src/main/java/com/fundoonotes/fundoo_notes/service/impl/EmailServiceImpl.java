package com.fundoonotes.fundoo_notes.service.impl;

import com.fundoonotes.fundoo_notes.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        String link = "http://localhost:8080" +
                "/api/users/verify?token=" + token;
        sendEmail(toEmail,
                "Verify Your Fundoo Notes Account",
                "Hello,\n\nClick to verify your account:\n\n"
                        + link + "\n\nThis link expires in 24 hours.\n\n"
                        + "Regards,\nFundoo Notes Team");
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = "http://localhost:4200" +
                "/reset-password?token=" + token;
        sendEmail(toEmail,
                "Reset Your Fundoo Notes Password",
                "Hello,\n\nClick to reset your password:\n\n"
                        + link + "\n\nThis link expires in 24 hours.\n\n"
                        + "Regards,\nFundoo Notes Team");
    }

    @Override
    public void sendReminderEmail(String toEmail, String noteTitle) {
        sendReminderEmail(toEmail, noteTitle, "");
    }

    @Override
    public void sendReminderEmail(String toEmail, String noteTitle, String noteContent) {
        String title = (noteTitle != null && !noteTitle.trim().isEmpty()) ? noteTitle : "Untitled Note";
        String contentText = (noteContent != null && !noteContent.trim().isEmpty()) ? noteContent : "";
        String dashboardUrl = "http://localhost:4200/dashboard";

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
        } catch (Exception e) {
            sendEmail(toEmail,
                    "Reminder: " + title,
                    "Hello,\n\nThis is a reminder for your note:\n\n\"" + title + "\"\n"
                            + (contentText.isEmpty() ? "" : "\nContent:\n" + contentText + "\n")
                            + "\nPlease check your Fundoo Notes.\n\nRegards,\nFundoo Notes Team");
        }
    }

    @Override
    public void sendCollaborationEmail(String toEmail, String ownerEmail, String noteTitle, Long noteId) {
        String dashboardUrl = "http://localhost:4200/dashboard?noteId=" + (noteId != null ? noteId : "");
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
        } catch (Exception e) {
            // Fallback to simple mail message if MimeMessage fails
            sendEmail(toEmail,
                    "Note shared with you - Fundoo Notes",
                    ownerEmail + " shared a note with you: \"" + title + "\"\n\nOpen note here:\n" + dashboardUrl);
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