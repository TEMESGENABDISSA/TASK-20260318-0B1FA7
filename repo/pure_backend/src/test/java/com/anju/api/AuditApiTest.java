package com.anju.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anju.audit.AuditLog;
import com.anju.audit.AuditLogRepository;
import com.anju.common.GlobalExceptionHandler;
import com.anju.controller.AuditController;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuditController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuditApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @Test
    void listLogsShouldReturnPagedResult() throws Exception {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setOperator("admin");
        log.setAction("APPOINTMENT_CREATE");
        log.setEntityType("Appointment");
        log.setEntityId("AP-1");
        log.setOccurredAt(LocalDateTime.now());

        when(auditLogRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 50), 1));

        mockMvc.perform(get("/api/v1/audit/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(1));
    }
}

