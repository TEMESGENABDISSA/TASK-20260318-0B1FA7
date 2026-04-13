package com.anju.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anju.common.BusinessException;
import com.anju.common.GlobalExceptionHandler;
import com.anju.controller.AuthController;
import com.anju.dto.LoginResponse;
import com.anju.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void loginSuccess() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse(
                "token-123", 7200L, "admin", Set.of("ADMIN")
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("admin", "Admin1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("token-123"))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void loginFailure() throws Exception {
        when(authService.login(any())).thenThrow(new BusinessException("invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("admin", "bad"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("invalid credentials"));
    }

    private record LoginPayload(String username, String password) {
    }
}
