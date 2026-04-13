package com.anju.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anju.common.GlobalExceptionHandler;
import com.anju.config.SecurityConfig;
import com.anju.controller.AppointmentController;
import com.anju.service.AppointmentService;
import com.anju.service.IdempotencyService;
import com.anju.service.ImportExportValidationService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AppointmentController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, BearerTokenAuthenticationFilter.class, TokenStore.class, GlobalExceptionHandler.class})
class AppointmentRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenStore tokenStore;

    @MockBean
    private AppointmentService appointmentService;
    @MockBean
    private IdempotencyService idempotencyService;
    @MockBean
    private ImportExportValidationService importExportValidationService;

    @Test
    void listShouldBeForbiddenWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listShouldAllowDispatcherRole() throws Exception {
        when(appointmentService.list()).thenReturn(List.of());
        String token = tokenStore.issueToken("u1", Set.of("DISPATCHER"), 60).token();

        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}

