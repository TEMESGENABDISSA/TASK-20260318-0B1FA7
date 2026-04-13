package com.anju.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anju.audit.AuditLogService;
import com.anju.common.BusinessException;
import com.anju.dto.AppointmentCreateRequest;
import com.anju.dto.AppointmentRescheduleRequest;
import com.anju.entity.Appointment;
import com.anju.entity.AppointmentStatus;
import com.anju.repository.AppointmentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentStateMachine stateMachine;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createShouldFailWhenDurationInvalid() {
        AppointmentCreateRequest request = baseCreateRequest();
        request.setDurationMinutes(25);

        assertThrows(BusinessException.class, () -> appointmentService.create(request));
    }

    @Test
    void createShouldFailWhenStaffConflictExists() {
        AppointmentCreateRequest request = baseCreateRequest();
        when(appointmentRepository.existsStaffConflict(anyLong(), any(), any(), eq(null), anyCollection()))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> appointmentService.create(request));
    }

    @Test
    void rescheduleShouldFailWhenLimitExceeded() {
        Appointment appointment = appointmentWithStatus(AppointmentStatus.CONFIRMED);
        appointment.setRescheduleCount(2);
        when(appointmentRepository.lockByAppointmentNo("AP-1")).thenReturn(Optional.of(appointment));

        AppointmentRescheduleRequest request = new AppointmentRescheduleRequest();
        request.setNewStartTime(LocalDateTime.now().plusDays(2));
        request.setDurationMinutes(60);

        assertThrows(BusinessException.class, () -> appointmentService.reschedule("AP-1", request));
    }

    @Test
    void cancelShouldApplyPenaltyCapWithin24Hours() {
        Appointment appointment = appointmentWithStatus(AppointmentStatus.CONFIRMED);
        appointment.setOrderAmount(new BigDecimal("1000.00"));
        appointment.setStartTime(LocalDateTime.now().plusHours(6));
        when(appointmentRepository.lockByAppointmentNo("AP-1")).thenReturn(Optional.of(appointment));

        Appointment saved = appointmentService.cancel("AP-1");

        assertEquals(new BigDecimal("50.00"), saved.getPenaltyAmount());
        assertEquals(AppointmentStatus.CANCELLED, saved.getStatus());
        verify(auditLogService).logAction(eq("APPOINTMENT_CANCEL"), anyString(), anyString(), any(), any());
    }

    @Test
    void autoReleaseShouldReleaseExpiredPendingAppointments() {
        Appointment pending = appointmentWithStatus(AppointmentStatus.PENDING_CONFIRMATION);
        pending.setAppointmentNo("AP-1");
        when(appointmentRepository.findExpiredPending(eq(AppointmentStatus.PENDING_CONFIRMATION), any()))
                .thenReturn(List.of(pending));

        appointmentService.autoReleaseUnconfirmedAppointments();

        assertEquals(AppointmentStatus.EXPIRED_RELEASED, pending.getStatus());
        verify(appointmentRepository).save(pending);
    }

    private AppointmentCreateRequest baseCreateRequest() {
        AppointmentCreateRequest request = new AppointmentCreateRequest();
        request.setAppointmentNo("AP-1");
        request.setPropertyId(1L);
        request.setServiceType("STANDARD");
        request.setDurationMinutes(60);
        request.setStaffId(11L);
        request.setResourceId(21L);
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setOrderAmount(new BigDecimal("200.00"));
        return request;
    }

    private Appointment appointmentWithStatus(AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setAppointmentNo("AP-1");
        appointment.setPropertyId(1L);
        appointment.setStatus(status);
        appointment.setServiceType("STANDARD");
        appointment.setDurationMinutes(60);
        appointment.setStaffId(11L);
        appointment.setResourceId(21L);
        appointment.setStartTime(LocalDateTime.now().plusDays(2));
        appointment.setEndTime(LocalDateTime.now().plusDays(2).plusMinutes(60));
        appointment.setOrderAmount(new BigDecimal("200.00"));
        appointment.setPenaltyAmount(BigDecimal.ZERO);
        appointment.setRescheduleCount(0);
        appointment.setConfirmDeadline(LocalDateTime.now().plusMinutes(15));
        return appointment;
    }
}
