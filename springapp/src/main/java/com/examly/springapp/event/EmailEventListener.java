package com.examly.springapp.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.examly.springapp.email.AdminInviteEmail;
import com.examly.springapp.email.BookingStatusEmail;
import com.examly.springapp.email.EmailMessage;
import com.examly.springapp.email.OtpEmail;
import com.examly.springapp.email.PasswordResetEmail;
import com.examly.springapp.exceptions.EmailSendException;
import com.examly.springapp.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// event -> email, only after the commit and on an email thread: the request never waits for gmail
// a rolled back transaction sends nothing, a gmail problem only goes to the log
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailService emailService;

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOtpRequested(OtpRequested event) {
        EmailMessage email = "SIGNUP".equals(event.purpose())
                ? new OtpEmail(event.username(), event.code(), event.minutes())
                : new PasswordResetEmail(event.username(), event.code(), event.minutes());
        send(event.email(), email);
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAdminInvited(AdminInvited event) {
        send(event.email(), new AdminInviteEmail(event.username(), event.link(), event.hours()));
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingDecided(BookingDecided event) {
        send(event.driverEmail(), new BookingStatusEmail(event.driverName(), event.trackName(), event.sessionDate(),
                event.startTime(), event.endTime(), event.carNumber(), event.status(), event.note(),
                event.refundAmount()));
    }

    private void send(String to, EmailMessage email) {
        try {
            emailService.send(to, email);
        } catch (EmailSendException e) {
            log.error("Email '{}' to {} could not be sent: {}", email.subject(), to, e.getMessage());
        }
    }
}
