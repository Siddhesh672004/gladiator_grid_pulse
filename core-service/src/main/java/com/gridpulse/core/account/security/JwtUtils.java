package com.gridpulse.core.account.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.gridpulse.core.account.data.User;
import com.gridpulse.core.platform.properties.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// creates and reads the jwt token
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;

    private byte[] signingKey;

    // the key must survive restarts (devtools restarts the app often), else everybody gets logged out
    @PostConstruct
    public void init() {
        // JWT_SECRET filled -> that is the key, else the key file
        signingKey = Optional.ofNullable(jwtProperties.secret())
                .filter(secret -> !secret.isBlank())
                .map(this::keyFromSecret)
                .orElseGet(this::keyFromFile);
    }

    private byte[] keyFromFile() {
        log.warn("JWT key from {} - set JWT_SECRET: a second instance or service will reject these tokens",
                jwtProperties.secretFile());
        return loadOrCreateKey(Path.of(jwtProperties.secretFile()));
    }

    private byte[] keyFromSecret(String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        log.info("JWT signing key loaded from JWT_SECRET");
        return key;
    }

    private byte[] loadOrCreateKey(Path file) {
        try {
            byte[] key = randomKey();
            Files.createDirectories(file.getParent());
            Files.writeString(file, Base64.getEncoder().encodeToString(key), StandardOpenOption.CREATE_NEW);
            file.toFile().setReadable(false, false);
            file.toFile().setReadable(true, true);
            log.info("JWT signing key created in {}", file);
            return key;
        } catch (FileAlreadyExistsException e) {
            return readKey(file);
        } catch (IOException e) {
            return randomKeyForThisRun(e);
        }
    }

    private byte[] readKey(Path file) {
        try {
            byte[] key = Base64.getDecoder().decode(Files.readString(file).trim());
            log.info("JWT signing key loaded from {}", file);
            return key;
        } catch (IOException | IllegalArgumentException e) {
            return randomKeyForThisRun(e);
        }
    }

    private byte[] randomKeyForThisRun(Exception reason) {
        log.warn("JWT key file not usable ({}) - random key for this run only, delete {} to make a new one",
                reason.getClass().getSimpleName(), jwtProperties.secretFile());
        return randomKey();
    }

    private byte[] randomKey() {
        byte[] key = new byte[64];
        new SecureRandom().nextBytes(key);
        return key;
    }

    // subject = email, the next three claims are read by angular (jwt-decode), "pw" only by validateToken
    public String generateToken(User user) {
        return Jwts.builder()
                .claim("userId", user.getUserId())
                .claim("username", user.getUsername())
                .claim("userRole", user.getUserRole().getLabel())
                .claim("pw", passwordStamp(user.getPassword()))
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.expirationMs()))
                .signWith(SignatureAlgorithm.HS256, signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(signingKey)
                .parseClaimsJws(token)
                .getBody();
    }

    // same email AND same user id: a deleted account's old token must not open a new account with the same email
    // same password stamp: a token made before a password reset stops working
    public boolean validateToken(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        boolean expired = claims.getExpiration().before(new Date());
        boolean sameEmail = claims.getSubject().equalsIgnoreCase(userDetails.getUsername());
        boolean sameAccount = userDetails instanceof UserPrinciple principle
                && claims.get("userId", Integer.class) == principle.getUserId();
        boolean samePassword = passwordStamp(userDetails.getPassword()).equals(claims.get("pw", String.class));
        return sameEmail && sameAccount && samePassword && !expired;
    }

    // a few letters made from the stored BCrypt hash, they change whenever the password changes
    private String passwordStamp(String passwordHash) {
        return Integer.toHexString(passwordHash.hashCode());
    }
}
