package com.gridpulse.notification.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.gridpulse.notification.config.properties.EmailProperties;
import com.gridpulse.notification.email.Attachment;
import com.gridpulse.notification.email.EmailMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// test runs only (MAIL_TO_FILE=true): each email becomes a text file in target/test-mails, its files lie next to it
// nothing goes to gmail. not a @Service, config/EmailConfig creates it in that test mode
@Slf4j
@RequiredArgsConstructor
public class FileEmailService implements EmailService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final EmailProperties emailProperties;

    @Override
    public void send(String to, EmailMessage message, List<Attachment> attachments) {
        String name = LocalDateTime.now().format(FILE_TIME) + "_" + to;
        Path folder = Path.of(emailProperties.fileDir());
        String text = "To: " + to + "\nSubject: " + message.subject() + "\n\n" + message.body();
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(name + ".txt"), text, StandardCharsets.UTF_8);
            for (Attachment file : attachments) {
                Files.write(folder.resolve(name + "_" + file.filename()), file.data());
            }
            // subject only, the body holds a code
            log.info("TEST MODE: email '{}' for {} written to {} ({} attachment(s))", message.subject(), to, folder,
                    attachments.size());
        } catch (IOException e) {
            throw new EmailSendException("Could not write the email file", e);
        }
    }
}
