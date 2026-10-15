package com.examly.springapp.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.config.properties.OtpProperties;
import com.examly.springapp.exceptions.OtpException;
import com.examly.springapp.model.OtpCode;
import com.examly.springapp.model.OtpPurpose;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.OtpCodeRepo;
import com.gridpulse.common.event.OtpRequested;

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
    private final ApplicationEventPublisher eventPublisher;
    private final OtpProperties otpProperties;

    // the code goes to kafka with this transaction (messaging/DomainEventOutboxListener), notification-service emails it
    @Override
    @Transactional
    public int createAndSend(User user, OtpPurpose purpose) {
        closeOlder(user.getEmail(), purpose);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        int minutes = purpose == OtpPurpose.SIGNUP ? otpProperties.signupMinutes() : otpProperties.resetMinutes();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(minutes));
        otpCodeRepo.save(new OtpCode(user.getEmail(), purpose, passwordEncoder.encode(code), expiresAt));

        eventPublisher.publishEvent(
                new OtpRequested(user.getEmail(), user.getUsername(), code, minutes, purpose.name()));
        log.info("{} code made for {}", purpose, user.getEmail());
        return minutes * 60;
    }

    // 32 random bytes as url-safe text: the link can't be guessed
    @Override
    @Transactional
    public String createInvite(User admin) {
        closeOlder(admin.getEmail(), OtpPurpose.ADMIN_INVITE);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(Duration.ofHours(otpProperties.inviteHours()));
        otpCodeRepo.save(new OtpCode(admin.getEmail(), OtpPurpose.ADMIN_INVITE, passwordEncoder.encode(token), expiresAt));
        log.info("Invite link made for admin {}", admin.getEmail());
        return token;
    }

    // a new code closes the older ones
    private void closeOlder(String email, OtpPurpose purpose) {
        List<OtpCode> older = otpCodeRepo.findByEmailAndPurposeAndUsedFalse(email, purpose).stream()
                .peek(old -> old.setUsed(true))
                .toList();
        otpCodeRepo.saveAll(older);
    }

    // not @Transactional on purpose: a wrong try must be saved even though we throw
    @Override
    public void verify(String email, OtpPurpose purpose, String code) {
        String what = what(purpose);
        OtpCode otp = lastCode(email, purpose)
                .orElseThrow(() -> new OtpException("No " + what + " was sent to this email. Please ask for a new one."));
        // used, expired, too many tries: the first one that is true is the answer
        Stream.of(new Rule(otp.isUsed(), "This " + what + " was already used. Please ask for a new one."),
                        new Rule(otp.isExpired(), "This " + what + " has expired. Please ask for a new one."),
                        new Rule(otp.getAttempts() >= otpProperties.maxAttempts(),
                                "Too many wrong tries. Please ask for a new " + what + "."))
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
                ? "Wrong " + what(purpose) + ". " + left + (left == 1 ? " try left." : " tries left.")
                : "Too many wrong tries. Please ask for a new " + what(purpose) + ".");
    }

    // the word the visitor sees: the emailed 6 digits or the invite link
    private static String what(OtpPurpose purpose) {
        return purpose == OtpPurpose.ADMIN_INVITE ? "invite link" : "code";
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
