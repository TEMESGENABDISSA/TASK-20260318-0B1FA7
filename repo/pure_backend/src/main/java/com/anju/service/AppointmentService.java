package com.anju.service;

import com.anju.audit.AuditLogService;
import com.anju.common.BusinessException;
import com.anju.dto.AppointmentCreateRequest;
import com.anju.dto.AppointmentRescheduleRequest;
import com.anju.entity.Appointment;
import com.anju.entity.AppointmentStatus;
import com.anju.repository.AppointmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final int MAX_RESCHEDULE_COUNT = 2;
    private static final BigDecimal CANCELLATION_RATE = new BigDecimal("0.10");
    private static final BigDecimal CANCELLATION_CAP = new BigDecimal("50.00");
    private static final int CONFIRMATION_TIMEOUT_MINUTES = 15;
    private static final List<Integer> ALLOWED_DURATIONS = List.of(15, 30, 60, 90);
    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
            AppointmentStatus.CREATED,
            AppointmentStatus.PENDING_CONFIRMATION,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.IN_SERVICE
    );

    private final AppointmentRepository appointmentRepository;
    private final AppointmentStateMachine stateMachine;
    private final AuditLogService auditLogService;

    @Transactional
    public Appointment create(AppointmentCreateRequest request) {
        validateCreateRequest(request);
        LocalDateTime endTime = request.getStartTime().plusMinutes(request.getDurationMinutes());
        checkConflict(request.getStaffId(), request.getResourceId(), request.getStartTime(), endTime, null);

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(request.getAppointmentNo());
        appointment.setPropertyId(request.getPropertyId());
        appointment.setServiceType(request.getServiceType());
        appointment.setDurationMinutes(request.getDurationMinutes());
        appointment.setStaffId(request.getStaffId());
        appointment.setResourceId(request.getResourceId());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(endTime);
        appointment.setOrderAmount(defaultAmount(request.getOrderAmount()));
        appointment.setContactPhone(request.getContactPhone());
        appointment.setIdNo(request.getIdNo());
        appointment.setPenaltyAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        appointment.setRescheduleCount(0);
        appointment.setStatus(AppointmentStatus.PENDING_CONFIRMATION);
        appointment.setConfirmDeadline(LocalDateTime.now().plusMinutes(CONFIRMATION_TIMEOUT_MINUTES));

        Appointment saved = appointmentRepository.save(appointment);
        auditLogService.logAction("APPOINTMENT_CREATE", "Appointment", saved.getAppointmentNo(), null, saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Appointment getByNo(String appointmentNo) {
        return appointmentRepository.findByAppointmentNo(appointmentNo)
                .orElseThrow(() -> new BusinessException("Appointment not found: " + appointmentNo));
    }

    @Transactional(readOnly = true)
    public List<Appointment> list() {
        return appointmentRepository.findAll().stream().collect(Collectors.toList());
    }

    @Transactional
    public Appointment confirm(String appointmentNo) {
        Appointment appointment = loadAndLock(appointmentNo);
        Appointment before = copyAppointment(appointment);
        stateMachine.validateTransition(appointment, AppointmentStatus.CONFIRMED, LocalDateTime.now());
        if (appointment.getStatus() == AppointmentStatus.CREATED) {
            appointment.setStatus(AppointmentStatus.PENDING_CONFIRMATION);
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appointment);
        auditLogService.logAction("APPOINTMENT_CONFIRM", "Appointment", saved.getAppointmentNo(), before, saved);
        return saved;
    }

    @Transactional
    public Appointment startService(String appointmentNo) {
        Appointment appointment = loadAndLock(appointmentNo);
        Appointment before = copyAppointment(appointment);
        stateMachine.validateTransition(appointment, AppointmentStatus.IN_SERVICE, LocalDateTime.now());
        appointment.setStatus(AppointmentStatus.IN_SERVICE);
        Appointment saved = appointmentRepository.save(appointment);
        auditLogService.logAction("APPOINTMENT_START_SERVICE", "Appointment", saved.getAppointmentNo(), before, saved);
        return saved;
    }

    @Transactional
    public Appointment complete(String appointmentNo) {
        Appointment appointment = loadAndLock(appointmentNo);
        Appointment before = copyAppointment(appointment);
        stateMachine.validateTransition(appointment, AppointmentStatus.COMPLETED, LocalDateTime.now());
        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment saved = appointmentRepository.save(appointment);
        auditLogService.logAction("APPOINTMENT_COMPLETE", "Appointment", saved.getAppointmentNo(), before, saved);
        return saved;
    }

    @Transactional
    public Appointment reschedule(String appointmentNo, AppointmentRescheduleRequest request) {
        Appointment appointment = loadAndLock(appointmentNo);
        Appointment before = copyAppointment(appointment);
        validateRescheduleRequest(appointment, request);

        LocalDateTime newEnd = request.getNewStartTime().plusMinutes(request.getDurationMinutes());
        checkConflict(
                appointment.getStaffId(),
                appointment.getResourceId(),
                request.getNewStartTime(),
                newEnd,
                appointment.getId()
        );

        appointment.setStartTime(request.getNewStartTime());
        appointment.setEndTime(newEnd);
        appointment.setDurationMinutes(request.getDurationMinutes());
        appointment.setRescheduleCount(appointment.getRescheduleCount() + 1);
        Appointment saved = appointmentRepository.save(appointment);
        auditLogService.logAction("APPOINTMENT_RESCHEDULE", "Appointment", saved.getAppointmentNo(), before, saved);
        return saved;
    }

    @Transactional
    public Appointment cancel(String appointmentNo) {
        Appointment appointment = loadAndLock(appointmentNo);
        Appointment before = copyAppointment(appointment);
        stateMachine.validateTransition(appointment, AppointmentStatus.CANCELLED, LocalDateTime.now());

        long hoursBeforeStart = ChronoUnit.HOURS.between(LocalDateTime.now(), appointment.getStartTime());
        BigDecimal penalty = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (hoursBeforeStart < 24) {
            penalty = calculatePenalty(appointment.getOrderAmount());
        }

        appointment.setPenaltyAmount(penalty);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);
        auditLogService.logAction("APPOINTMENT_CANCEL", "Appointment", saved.getAppointmentNo(), before, saved);
        return saved;
    }

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void autoReleaseUnconfirmedAppointments() {
        releaseExpiredPendingBefore(LocalDateTime.now());
    }

    @Transactional
    public int releaseExpiredPendingBefore(LocalDateTime beforeTime) {
        List<Appointment> expired = appointmentRepository
                .findExpiredPending(AppointmentStatus.PENDING_CONFIRMATION, beforeTime == null ? LocalDateTime.now() : beforeTime)
                .stream()
                .toList();
        for (Appointment item : expired) {
            item.setStatus(AppointmentStatus.EXPIRED_RELEASED);
            Appointment saved = appointmentRepository.save(item);
            auditLogService.logAction("APPOINTMENT_AUTO_RELEASE", "Appointment", saved.getAppointmentNo(), null, saved);
        }
        return expired.size();
    }

    private void validateCreateRequest(AppointmentCreateRequest request) {
        if (request.getAppointmentNo() == null || request.getAppointmentNo().isBlank()) {
            throw new BusinessException("appointmentNo is required");
        }
        if (request.getPropertyId() == null || request.getPropertyId() <= 0) {
            throw new BusinessException("propertyId must be positive");
        }
        if (request.getStaffId() == null || request.getStaffId() <= 0) {
            throw new BusinessException("staffId must be positive");
        }
        if (request.getResourceId() == null || request.getResourceId() <= 0) {
            throw new BusinessException("resourceId must be positive");
        }
        if (request.getStartTime() == null || !request.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("startTime must be in the future");
        }
        if (request.getDurationMinutes() == null || !ALLOWED_DURATIONS.contains(request.getDurationMinutes())) {
            throw new BusinessException("durationMinutes must be one of 15/30/60/90");
        }
    }

    private void validateRescheduleRequest(Appointment appointment, AppointmentRescheduleRequest request) {
        if (appointment.getRescheduleCount() >= MAX_RESCHEDULE_COUNT) {
            throw new BusinessException("Reschedule limit exceeded (max 2)");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING_CONFIRMATION
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException("Appointment can only be rescheduled from pending/confirmed status");
        }
        if (request.getNewStartTime() == null) {
            throw new BusinessException("newStartTime is required");
        }
        if (request.getDurationMinutes() == null || !ALLOWED_DURATIONS.contains(request.getDurationMinutes())) {
            throw new BusinessException("durationMinutes must be one of 15/30/60/90");
        }
        long hoursBeforeStart = ChronoUnit.HOURS.between(LocalDateTime.now(), appointment.getStartTime());
        if (hoursBeforeStart < 24) {
            throw new BusinessException("Reschedule must be requested at least 24 hours before start time");
        }
    }

    private void checkConflict(Long staffId, Long resourceId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        boolean staffConflict = appointmentRepository.existsStaffConflict(
                staffId, start, end, excludeId, ACTIVE_STATUSES
        );
        if (staffConflict) {
            throw new BusinessException("Staff is already booked in the requested time range");
        }

        boolean resourceConflict = appointmentRepository.existsResourceConflict(
                resourceId, start, end, excludeId, ACTIVE_STATUSES
        );
        if (resourceConflict) {
            throw new BusinessException("Resource is already booked in the requested time range");
        }
    }

    private Appointment loadAndLock(String appointmentNo) {
        return appointmentRepository.lockByAppointmentNo(appointmentNo)
                .orElseThrow(() -> new BusinessException("Appointment not found: " + appointmentNo));
    }

    private BigDecimal calculatePenalty(BigDecimal orderAmount) {
        BigDecimal safeAmount = defaultAmount(orderAmount);
        BigDecimal byRate = safeAmount.multiply(CANCELLATION_RATE).setScale(2, RoundingMode.HALF_UP);
        return byRate.min(CANCELLATION_CAP);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private Appointment copyAppointment(Appointment source) {
        Appointment target = new Appointment();
        target.setId(source.getId());
        target.setAppointmentNo(source.getAppointmentNo());
        target.setPropertyId(source.getPropertyId());
        target.setStatus(source.getStatus());
        target.setServiceType(source.getServiceType());
        target.setDurationMinutes(source.getDurationMinutes());
        target.setStaffId(source.getStaffId());
        target.setResourceId(source.getResourceId());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setOrderAmount(source.getOrderAmount());
        target.setPenaltyAmount(source.getPenaltyAmount());
        target.setRescheduleCount(source.getRescheduleCount());
        target.setConfirmDeadline(source.getConfirmDeadline());
        target.setContactPhone(source.getContactPhone());
        target.setIdNo(source.getIdNo());
        return target;
    }
}
