package com.anju.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anju.common.BusinessException;
import com.anju.common.GlobalExceptionHandler;
import com.anju.controller.AppointmentController;
import com.anju.entity.Appointment;
import com.anju.entity.AppointmentStatus;
import com.anju.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AppointmentApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    @Test
    void createAppointmentSuccess() throws Exception {
        Appointment response = appointment("AP-1001", AppointmentStatus.PENDING_CONFIRMATION);
        when(appointmentService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePayload(
                                "AP-1001", 1L, "STANDARD", 60, 11L, 21L,
                                LocalDateTime.now().plusDays(2), new BigDecimal("300.00")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentNo").value("AP-1001"))
                .andExpect(jsonPath("$.data.status").value("PENDING_CONFIRMATION"));
    }

    @Test
    void createAppointmentFailureConflict() throws Exception {
        when(appointmentService.create(any())).thenThrow(new BusinessException("Resource is already booked in the requested time range"));

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", "k-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePayload(
                                "AP-1001", 1L, "STANDARD", 60, 11L, 21L,
                                LocalDateTime.now().plusDays(2), new BigDecimal("300.00")
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("Resource is already booked in the requested time range"));
    }

    @Test
    void confirmAppointmentSuccess() throws Exception {
        Appointment response = appointment("AP-1001", AppointmentStatus.CONFIRMED);
        when(appointmentService.confirm(eq("AP-1001"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/appointments/AP-1001:confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void confirmAppointmentFailureInvalidState() throws Exception {
        when(appointmentService.confirm(eq("AP-1001"))).thenThrow(new BusinessException("Invalid state transition"));

        mockMvc.perform(post("/api/v1/appointments/AP-1001:confirm"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("Invalid state transition"));
    }

    private Appointment appointment(String no, AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setAppointmentNo(no);
        a.setStatus(status);
        a.setStartTime(LocalDateTime.now().plusDays(1));
        a.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        a.setOrderAmount(new BigDecimal("300.00"));
        return a;
    }

    private record CreatePayload(
            String appointmentNo,
            Long propertyId,
            String serviceType,
            Integer durationMinutes,
            Long staffId,
            Long resourceId,
            LocalDateTime startTime,
            BigDecimal orderAmount
    ) {
    }
}
