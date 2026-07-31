package com.fundoonotes.fundoo_notes.jms;

import com.fundoonotes.fundoo_notes.model.Note;
import com.fundoonotes.fundoo_notes.repository.NoteRepository;
import com.fundoonotes.fundoo_notes.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private EmailService emailService;

    // ReminderProducer NOT injected — RabbitMQ port 5671 is blocked on Render Free Tier.
    // Emails are sent directly via Brevo SMTP (port 587, allowed on Render).

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkReminders() {
        // Use Asia/Kolkata timezone because frontend sends IST local time.
        // On Render cloud servers, system clock defaults to UTC (5.5 hours behind IST).
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

        List<Note> dueNotes = noteRepository
                .findByReminderBeforeAndIsTrashedFalse(now);

        if (dueNotes.isEmpty()) {
            return;
        }

        log.info("Reminders check: Found {} reminder(s) due at {}", dueNotes.size(), LocalDateTime.now());

        for (Note note : dueNotes) {
            String userEmail = (note.getUser() != null) ? note.getUser().getEmail() : null;

            if (userEmail == null || userEmail.isBlank()) {
                log.warn("Skipping reminder for note ID {} — user email is missing.", note.getId());
                note.setReminder(null);
                noteRepository.save(note);
                continue;
            }

            String title   = (note.getTitle()   != null && !note.getTitle().isBlank())   ? note.getTitle()   : "Untitled Note";
            String content = (note.getContent()  != null)                                 ? note.getContent() : "";

            try {
                log.info("Sending reminder email to {} for note: '{}'", userEmail, title);
                emailService.sendReminderEmail(userEmail, title, content);
                log.info("Reminder email sent successfully to {}", userEmail);
            } catch (Exception e) {
                log.error("Failed to send reminder email to {}: {}", userEmail, e.getMessage(), e);
            }

            note.setReminder(null);
            noteRepository.save(note);
        }
    }
}