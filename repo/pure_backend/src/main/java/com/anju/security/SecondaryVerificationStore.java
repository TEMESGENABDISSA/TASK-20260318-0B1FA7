package com.anju.security;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SecondaryVerificationStore {

    private final Map<String, VerificationEntry> secondaryTokens = new ConcurrentHashMap<>();

    public String issue(String username, long ttlSeconds) {
        String token = UUID.randomUUID().toString();
        secondaryTokens.put(token, new VerificationEntry(username, LocalDateTime.now().plusSeconds(ttlSeconds)));
        return token;
    }

    public boolean validateAndConsume(String token, String username) {
        if (token == null || token.isBlank()) {
            return false;
        }
        VerificationEntry entry = secondaryTokens.get(token);
        if (entry == null) {
            return false;
        }
        if (!entry.username().equals(username) || entry.expiresAt().isBefore(LocalDateTime.now())) {
            secondaryTokens.remove(token);
            return false;
        }
        secondaryTokens.remove(token);
        return true;
    }

    private record VerificationEntry(String username, LocalDateTime expiresAt) {
    }
}
