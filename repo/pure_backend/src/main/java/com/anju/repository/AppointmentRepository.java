package com.anju.repository;

import com.anju.entity.Appointment;
import com.anju.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByAppointmentNo(String appointmentNo);

    @Query("""
        SELECT COUNT(a) > 0
        FROM Appointment a
        WHERE a.staffId = :staffId
          AND a.status IN :activeStatuses
          AND (:excludedId IS NULL OR a.id <> :excludedId)
          AND a.startTime < :newEnd
          AND a.endTime > :newStart
    """)
    boolean existsStaffConflict(
            @Param("staffId") Long staffId,
            @Param("newStart") LocalDateTime newStart,
            @Param("newEnd") LocalDateTime newEnd,
            @Param("excludedId") Long excludedId,
            @Param("activeStatuses") Collection<AppointmentStatus> activeStatuses
    );

    @Query("""
        SELECT COUNT(a) > 0
        FROM Appointment a
        WHERE a.resourceId = :resourceId
          AND a.status IN :activeStatuses
          AND (:excludedId IS NULL OR a.id <> :excludedId)
          AND a.startTime < :newEnd
          AND a.endTime > :newStart
    """)
    boolean existsResourceConflict(
            @Param("resourceId") Long resourceId,
            @Param("newStart") LocalDateTime newStart,
            @Param("newEnd") LocalDateTime newEnd,
            @Param("excludedId") Long excludedId,
            @Param("activeStatuses") Collection<AppointmentStatus> activeStatuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.appointmentNo = :appointmentNo
    """)
    Optional<Appointment> lockByAppointmentNo(@Param("appointmentNo") String appointmentNo);

    @Query("""
        SELECT a FROM Appointment a
        WHERE a.status = :status
          AND a.confirmDeadline <= :now
    """)
    Collection<Appointment> findExpiredPending(
            @Param("status") AppointmentStatus status,
            @Param("now") LocalDateTime now
    );
}
