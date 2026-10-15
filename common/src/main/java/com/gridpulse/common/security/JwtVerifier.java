package com.gridpulse.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

// checks a token made by the core's JwtUtils (HS256, key = JWT_SECRET as utf-8): every service needs the same JWT_SECRET
// only signature + expiry; whether the account is still enabled is the core's job
public class JwtVerifier {

    private final byte[] key;

    public JwtVerifier(String secret) {
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    // wrong signature, expired or not a token -> empty
    public Optional<JwtClaims> verify(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
            return Optional.of(new JwtClaims(claims.get("userId", Integer.class), claims.get("username", String.class),
                    claims.get("userRole", String.class), claims.getSubject(), claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
