package com.fundoonotes.fundoo_notes.jms;

import com.fundoonotes.fundoo_notes.model.Note;
import com.fundoonotes.fundoo_notes.repository.NoteRepository;
import com.fundoonotes.fundoo_notes.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private ReminderProducer reminderProducer;

    @Autowired
    private EmailService emailService;

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkReminders() {

        List<Note> dueNotes = noteRepository
                .findByReminderBeforeAndIsTrashedFalse(
                        LocalDateTime.now()
                );

        if (dueNotes.isEmpty()) {
            return;
        }

        System.out.println("Reminders check: Found " + dueNotes.size() + " reminder(s) due at " + LocalDateTime.now());

        for (Note note : dueNotes) {
            String userEmail = (note.getUser() != null) ? note.getUser().getEmail() : null;
            if (userEmail == null || userEmail.isBlank()) {
                System.out.println("Skipping reminder for note ID " + note.getId() + " because user email is missing.");
                note.setReminder(null);
                noteRepository.save(note);
                continue;
            }

            String title = (note.getTitle() != null && !note.getTitle().isBlank()) ? note.getTitle() : "Untitled Note";
            String content = (note.getContent() != null) ? note.getContent() : "";

            try {
                reminderProducer.sendReminder(userEmail, title, content);
            } catch (Exception e) {
                System.out.println("RabbitMQ dispatch unavailable (" + e.getMessage() + "). Sending direct email to " + userEmail);
                try {
                    emailService.sendReminderEmail(userEmail, title, content);
                } catch (Exception mailEx) {
                    System.err.println("Failed to send reminder email to " + userEmail + ": " + mailEx.getMessage());
                }
            }

            note.setReminder(null);
            noteRepository.save(note);
            System.out.println("Reminder successfully processed for note: " + title + " (User: " + userEmail + ")");
        }
    }
}