package com.anju.service;

import com.anju.common.BusinessException;
import com.anju.dto.LoginRequest;
import com.anju.dto.LoginResponse;
import com.anju.entity.UserAccount;
import com.anju.repository.UserAccountRepository;
import com.anju.security.SecondaryVerificationStore;
import com.anju.security.TokenStore;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final long ACCESS_TOKEN_TTL_SECONDS = 7200;
    private static final long SECONDARY_TOKEN_TTL_SECONDS = 300;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;
    private final SecondaryVerificationStore secondaryVerificationStore;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       TokenStore tokenStore,
                       SecondaryVerificationStore secondaryVerificationStore) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
        this.secondaryVerificationStore = secondaryVerificationStore;
    }

    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new BusinessException("username and password are required");
        }
        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("invalid credentials"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("invalid credentials");
        }
        Set<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        TokenStore.IssuedToken issuedToken = tokenStore.issueToken(user.getUsername(), roles, ACCESS_TOKEN_TTL_SECONDS);
        return new LoginResponse(issuedToken.token(), issuedToken.expiresInSeconds(), user.getUsername(), roles);
    }

    public String verifySecondaryPassword(String username, String secondaryPassword) {
        if (secondaryPassword == null || secondaryPassword.isBlank()) {
            throw new BusinessException("secondaryPassword is required");
        }
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("user not found"));
        if (!passwordEncoder.matches(secondaryPassword, user.getSecondaryPasswordHash())) {
            throw new BusinessException("secondary password invalid");
        }
        return secondaryVerificationStore.issue(username, SECONDARY_TOKEN_TTL_SECONDS);
    }

    public boolean validateSecondaryToken(String secondaryToken, String username) {
        return secondaryVerificationStore.validateAndConsume(secondaryToken, username);
    }
}
