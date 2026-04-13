package com.anju.service;

import com.anju.common.BusinessException;
import com.anju.entity.Appointment;
import com.anju.entity.AppointmentStatus;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AppointmentStateMachine {

    private final Map<AppointmentStatus, Set<AppointmentStatus>> transitions = new EnumMap<>(AppointmentStatus.class);

    public AppointmentStateMachine() {
        transitions.put(AppointmentStatus.CREATED, EnumSet.of(AppointmentStatus.PENDING_CONFIRMATION, AppointmentStatus.CANCELLED));
        transitions.put(AppointmentStatus.PENDING_CONFIRMATION, EnumSet.of(
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.CANCELLED,
                AppointmentStatus.EXPIRED_RELEASED
        ));
        transitions.put(AppointmentStatus.CONFIRMED, EnumSet.of(AppointmentStatus.IN_SERVICE, AppointmentStatus.CANCELLED));
        transitions.put(AppointmentStatus.IN_SERVICE, EnumSet.of(AppointmentStatus.COMPLETED));
        transitions.put(AppointmentStatus.CANCELLED, EnumSet.noneOf(AppointmentStatus.class));
        transitions.put(AppointmentStatus.COMPLETED, EnumSet.noneOf(AppointmentStatus.class));
        transitions.put(AppointmentStatus.EXPIRED_RELEASED, EnumSet.noneOf(AppointmentStatus.class));
    }

    public void validateTransition(Appointment appointment, AppointmentStatus targetStatus, LocalDateTime now) {
        AppointmentStatus current = appointment.getStatus();
        Set<AppointmentStatus> allowed = transitions.getOrDefault(current, EnumSet.noneOf(AppointmentStatus.class));
        if (!allowed.contains(targetStatus)) {
            throw new BusinessException("Invalid state transition: " + current + " -> " + targetStatus);
        }

        if (targetStatus == AppointmentStatus.CANCELLED) {
            long hoursBeforeStart = ChronoUnit.HOURS.between(now, appointment.getStartTime());
            if (hoursBeforeStart < 0) {
                throw new BusinessException("Cannot cancel appointment after start time");
            }
        }

        if (targetStatus == AppointmentStatus.COMPLETED && now.isBefore(appointment.getStartTime())) {
            throw new BusinessException("Appointment cannot be completed before start time");
        }

        if (targetStatus == AppointmentStatus.IN_SERVICE && now.isBefore(appointment.getStartTime())) {
            throw new BusinessException("Service cannot start before appointment start time");
        }
    }
}
