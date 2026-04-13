package com.anju.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.anju.common.BusinessException;
import com.anju.entity.Appointment;
import com.anju.entity.AppointmentStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppointmentStateMachineTest {

    private AppointmentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new AppointmentStateMachine();
    }

    @Test
    void shouldAllowCreatedToPendingConfirmation() {
        Appointment appointment = baseAppointment();
        appointment.setStatus(AppointmentStatus.CREATED);

        assertDoesNotThrow(() ->
                stateMachine.validateTransition(appointment, AppointmentStatus.PENDING_CONFIRMATION, LocalDateTime.now()));
    }

    @Test
    void shouldRejectInvalidTransitionCreatedToCompleted() {
        Appointment appointment = baseAppointment();
        appointment.setStatus(AppointmentStatus.CREATED);

        assertThrows(BusinessException.class, () ->
                stateMachine.validateTransition(appointment, AppointmentStatus.COMPLETED, LocalDateTime.now()));
    }

    @Test
    void shouldRejectCancelAfterAppointmentStarted() {
        Appointment appointment = baseAppointment();
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setStartTime(LocalDateTime.now().minusHours(1));

        assertThrows(BusinessException.class, () ->
                stateMachine.validateTransition(appointment, AppointmentStatus.CANCELLED, LocalDateTime.now()));
    }

    @Test
    void shouldRejectCompleteBeforeStartTime() {
        Appointment appointment = baseAppointment();
        appointment.setStatus(AppointmentStatus.IN_SERVICE);
        appointment.setStartTime(LocalDateTime.now().plusHours(2));

        assertThrows(BusinessException.class, () ->
                stateMachine.validateTransition(appointment, AppointmentStatus.COMPLETED, LocalDateTime.now()));
    }

    @Test
    void shouldAllowConfirmedToInServiceAtOrAfterStart() {
        Appointment appointment = baseAppointment();
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setStartTime(LocalDateTime.now().minusMinutes(1));

        assertDoesNotThrow(() ->
                stateMachine.validateTransition(appointment, AppointmentStatus.IN_SERVICE, LocalDateTime.now()));
    }

    private Appointment baseAppointment() {
        Appointment appointment = new Appointment();
        appointment.setStartTime(LocalDateTime.now().plusDays(1));
        return appointment;
    }
}
