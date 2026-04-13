package com.anju.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anju.common.GlobalExceptionHandler;
import com.anju.config.SecurityConfig;
import com.anju.controller.FinancialController;
import com.anju.entity.FinancialTransaction;
import com.anju.service.FinancialService;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FinancialController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, BearerTokenAuthenticationFilter.class, TokenStore.class, GlobalExceptionHandler.class})
class FinanceRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenStore tokenStore;

    @MockBean
    private FinancialService financialService;
    @MockBean
    private IdempotencyService idempotencyService;
    @MockBean
    private ImportExportValidationService importExportValidationService;

    @Test
    void financeShouldRejectNonFinanceRole() throws Exception {
        String token = tokenStore.issueToken("u1", Set.of("DISPATCHER"), 60).token();
        mockMvc.perform(get("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void financeShouldAllowFinanceRole() throws Exception {
        when(financialService.listTransactions(any(), any())).thenReturn(List.of(new FinancialTransaction()));
        String token = tokenStore.issueToken("u1", Set.of("FINANCE"), 60).token();
        mockMvc.perform(get("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}

