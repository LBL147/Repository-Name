package com.icinfo.taskmanagement.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

    public String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return hash(rawPassword).equalsIgnoreCase(encodedPassword);
    }
}
