package com.examly.springapp.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.config.properties.OtpProperties;
import com.examly.springapp.email.EmailMessage;
import com.examly.springapp.email.OtpEmail;
import com.examly.springapp.email.PasswordResetEmail;
import com.examly.springapp.exceptions.OtpException;
import com.examly.springapp.model.OtpCode;
import com.examly.springapp.model.OtpPurpose;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.OtpCodeRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// only the BCrypt hash is stored, a new code closes the older ones, the code itself is never logged
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    // one reason a code can't be used any more
    private record Rule(boolean broken, String message) {
    }

    private final OtpCodeRepo otpCodeRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpProperties otpProperties;

    // email fails -> rollback, the new code is not saved and the old ones stay open
    @Override
    @Transactional
    public int createAndSend(User user, OtpPurpose purpose) {
        // a new code closes the older ones
        List<OtpCode> older = otpCodeRepo.findByEmailAndPurposeAndUsedFalse(user.getEmail(), purpose).stream()
                .peek(old -> old.setUsed(true))
                .toList();
        otpCodeRepo.saveAll(older);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        int minutes = purpose == OtpPurpose.SIGNUP ? otpProperties.signupMinutes() : otpProperties.resetMinutes();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(minutes));
        otpCodeRepo.save(new OtpCode(user.getEmail(), purpose, passwordEncoder.encode(code), expiresAt));

        EmailMessage email = purpose == OtpPurpose.SIGNUP
                ? new OtpEmail(user.getUsername(), code, minutes)
                : new PasswordResetEmail(user.getUsername(), code, minutes);
        emailService.send(user.getEmail(), email);
        log.info("{} code sent to {}", purpose, user.getEmail());
        return minutes * 60;
    }

    // not @Transactional on purpose: a wrong try must be saved even though we throw
    @Override
    public void verify(String email, OtpPurpose purpose, String code) {
        OtpCode otp = lastCode(email, purpose)
                .orElseThrow(() -> new OtpException("No code was sent to this email. Please ask for a new one."));
        // used, expired, too many tries: the first one that is true is the answer
        Stream.of(new Rule(otp.isUsed(), "This code was already used. Please ask for a new one."),
                        new Rule(otp.isExpired(), "This code has expired. Please ask for a new one."),
                        new Rule(otp.getAttempts() >= otpProperties.maxAttempts(),
                                "Too many wrong tries. Please ask for a new code."))
                .filter(Rule::broken)
                .map(Rule::message)
                .findFirst()
                .ifPresent(message -> {
                    throw new OtpException(message);
                });
        // a wrong code is counted and saved, the right one is used up
        Optional.of(otp)
                .filter(sent -> passwordEncoder.matches(code, sent.getCodeHash()))
                .orElseThrow(() -> wrongTry(otp, email, purpose));

        otp.setUsed(true);
        otpCodeRepo.save(otp);
    }

    private OtpException wrongTry(OtpCode otp, String email, OtpPurpose purpose) {
        otp.setAttempts(otp.getAttempts() + 1);
        otpCodeRepo.save(otp);
        int left = otpProperties.maxAttempts() - otp.getAttempts();
        log.warn("Wrong {} code for {} ({} tries left)", purpose, email, left);
        return new OtpException(left > 0
                ? "Wrong code. " + left + (left == 1 ? " try left." : " tries left.")
                : "Too many wrong tries. Please ask for a new code.");
    }

    @Override
    public long secondsUntilNextCode(String email, OtpPurpose purpose) {
        // no code sent yet -> no waiting
        return lastCode(email, purpose)
                .map(last -> Duration.between(last.getCreatedAt(), Instant.now()).toSeconds())
                .map(waited -> Math.max(otpProperties.resendSeconds() - waited, 0))
                .orElse(0L);
    }

    @Override
    public boolean hasUsableCode(String email, OtpPurpose purpose) {
        return lastCode(email, purpose)
                .filter(this::isUsable)
                .isPresent();
    }

    // the newest code for this email + purpose, empty when none was ever sent
    private Optional<OtpCode> lastCode(String email, OtpPurpose purpose) {
        return otpCodeRepo.findTopByEmailAndPurposeOrderByCreatedAtDescIdDesc(email, purpose);
    }

    private boolean isUsable(OtpCode otp) {
        return !otp.isUsed() && !otp.isExpired() && otp.getAttempts() < otpProperties.maxAttempts();
    }
}
