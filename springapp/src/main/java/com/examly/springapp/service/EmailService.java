package com.examly.springapp.service;

import com.examly.springapp.email.EmailMessage;

// SmtpEmailService sends through gmail, FileEmailService is only for test runs
public interface EmailService {

    void send(String to, EmailMessage message);
}
