package com.icinfo.taskmanagement.security;

import com.icinfo.taskmanagement.common.ErrorCode;
import com.icinfo.taskmanagement.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createToken(CurrentUser currentUser) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getExpirationSeconds());
        return Jwts.builder()
                .subject(currentUser.getUsername())
                .claim("id", currentUser.getId())
                .claim("username", currentUser.getUsername())
                .claim("role", currentUser.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey())
                .compact();
    }

    public CurrentUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number idClaim = claims.get("id", Number.class);
            Long id = idClaim.longValue();
            String username = claims.get("username", String.class);
            if (username == null || username.isBlank()) {
                username = claims.getSubject();
            }
            String role = claims.get("role", String.class);
            return new CurrentUser(id, username, role);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
