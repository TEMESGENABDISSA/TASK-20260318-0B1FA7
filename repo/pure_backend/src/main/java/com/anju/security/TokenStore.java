package com.anju.security;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TokenStore {

    private final Map<String, TokenPrincipal> tokens = new ConcurrentHashMap<>();

    public IssuedToken issueToken(String username, Set<String> roles, long ttlSeconds) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);
        tokens.put(token, new TokenPrincipal(username, roles, expiresAt));
        return new IssuedToken(token, ttlSeconds);
    }

    public Optional<TokenPrincipal> findValid(String token) {
        TokenPrincipal principal = tokens.get(token);
        if (principal == null) {
            return Optional.empty();
        }
        if (principal.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public void revoke(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }

    public record IssuedToken(String token, long expiresInSeconds) {
    }
}
