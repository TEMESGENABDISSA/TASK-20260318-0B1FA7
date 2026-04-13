package com.anju.security;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenPrincipal {
    private String username;
    private Set<String> roles;
    private LocalDateTime expiresAt;
}
