package com.anju.controller;

import com.anju.common.ApiResponse;
import com.anju.dto.LoginRequest;
import com.anju.dto.LoginResponse;
import com.anju.dto.SecondaryVerifyRequest;
import com.anju.security.TokenStore;
import com.anju.service.AuthService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenStore tokenStore;

    public AuthController(AuthService authService, TokenStore tokenStore) {
        this.authService = authService;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/secondary-verify")
    public ApiResponse<Map<String, String>> secondaryVerify(@RequestBody SecondaryVerifyRequest request,
                                                            Authentication authentication) {
        String secondaryToken = authService.verifySecondaryPassword(
                authentication.getName(),
                request.getSecondaryPassword()
        );
        return ApiResponse.ok(Map.of("secondaryToken", secondaryToken));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication authentication) {
        return ApiResponse.ok(Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities()
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout(Authentication authentication,
                                                   @RequestBody(required = false) Map<String, String> payload) {
        String auth = payload == null ? null : payload.get("accessToken");
        if (auth != null && !auth.isBlank()) {
            tokenStore.revoke(auth);
        }
        return ApiResponse.ok(Map.of("status", "LOGGED_OUT", "user", authentication.getName()));
    }
}
