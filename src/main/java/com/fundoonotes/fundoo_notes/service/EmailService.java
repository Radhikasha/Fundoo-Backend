package com.fundoonotes.fundoo_notes.service;

public interface EmailService {

    void sendVerificationEmail(String toEmail, String token);

    void sendPasswordResetEmail(String toEmail, String token);

    void sendReminderEmail(String toEmail, String noteTitle);

    void sendReminderEmail(String toEmail, String noteTitle, String noteContent);

    void sendCollaborationEmail(String toEmail, String ownerEmail, String noteTitle, Long noteId);
}