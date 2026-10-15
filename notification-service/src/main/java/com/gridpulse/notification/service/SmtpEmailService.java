package com.gridpulse.notification.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;


import com.gridpulse.notification.email.Attachment;
import com.gridpulse.notification.email.EmailMessage;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every email goes out through gmail smtp (MAIL_USERNAME / MAIL_PASSWORD in secrets.properties)
// when gmail says no, the log says why (explain) and the kafka listener tries again later. config/EmailConfig creates it
@Slf4j
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private static final String LIMIT_USED_UP =
            "Gmail's DAILY SENDING LIMIT for this account is used up. It opens again within 24 hours. "
            + "To send right now: put another Gmail address + its app password in secrets.properties and restart";
    private static final String LOGIN_REFUSED =
            "Gmail does NOT ACCEPT the login (MAIL_USERNAME + MAIL_PASSWORD in secrets.properties). Check: "
            + "1) MAIL_PASSWORD is a 16-letter Gmail APP password (Google Account > Security > 2-Step Verification > "
            + "App passwords), not the normal password. 2) it was made in the SAME Google account as MAIL_USERNAME. "
            + "3) it was not deleted since (changing the Google password deletes all app passwords)";
    private static final String NOT_REACHABLE =
            "smtp.gmail.com:587 CANNOT BE REACHED from this machine (no internet, or this network blocks outgoing mail)";

    // words in gmail's answer -> what to do about it, looked at in this order
    private static final List<Reason> REASONS = List.of(
            new Reason(List.of("5.4.5", "sending limit"), LIMIT_USED_UP),
            new Reason(List.of("535", "not accepted", "Authentication failed"), LOGIN_REFUSED),
            new Reason(List.of("connect", "UnknownHost", "timed out", "timeout"), NOT_REACHABLE));

    private final JavaMailSender mailSender;
    private final String fromAddress;

    @Override
    public void send(String to, EmailMessage message, List<Attachment> attachments) {
        try {
            MimeMessage mail = mimeMessage(to, message, attachments);
            sendWithOneRetry(mail);
            log.info("Email '{}' sent to {}{}", message.subject(), to,
                    attachments.isEmpty() ? "" : " with " + attachments.size() + " attachment(s)");
        } catch (MailException | MessagingException e) {
            log.error("EMAIL NOT SENT: '{}' to {} - {}", message.subject(), to, explain(e));
            throw new EmailSendException("Could not send the email", e);
        }
    }

    // plain text + the files (multipart only when there are files)
    private MimeMessage mimeMessage(String to, EmailMessage message, List<Attachment> attachments)
            throws MessagingException {
        MimeMessage mail = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mail, !attachments.isEmpty(), "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(message.subject());
        helper.setText(message.body(), false);
        for (Attachment file : attachments) {
            helper.addAttachment(file.filename(), new ByteArrayResource(file.data()), file.contentType());
        }
        return mail;
    }

    // retry once only when gmail could not be reached, never after a refusal (more failed logins)
    private void sendWithOneRetry(MimeMessage mail) {
        try {
            mailSender.send(mail);
        } catch (MailException firstTry) {
            // a refusal goes back to the caller as it is
            Optional.of(firstTry)
                    .filter(problem -> explain(problem).contains("CANNOT BE REACHED"))
                    .orElseThrow(() -> firstTry);
            log.warn("Gmail could not be reached once, trying again");
            mailSender.send(mail);
        }
    }

    // gmail's technical answer -> one sentence on what to do (safe to log, no password in it)
    public static String explain(Exception e) {
        // the messages of the exception and of every cause behind it, one after the other
        String text = Stream.<Throwable>iterate(e, Objects::nonNull, Throwable::getCause)
                .map(cause -> cause.getMessage() + " ")
                .collect(Collectors.joining());
        // first reason that fits gmail's answer wins, else the answer itself (300 characters at most)
        return REASONS.stream()
                .filter(reason -> reason.words().stream().anyMatch(text::contains))
                .map(Reason::whatToDo)
                .findFirst()
                .orElseGet(() -> text.length() > 300 ? text.substring(0, 300) : text);
    }

    private record Reason(List<String> words, String whatToDo) {
    }
}
