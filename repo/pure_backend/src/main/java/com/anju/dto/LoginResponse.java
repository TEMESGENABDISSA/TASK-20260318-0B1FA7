package com.anju.dto;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private long expiresInSeconds;
    private String username;
    private Set<String> roles;
}
