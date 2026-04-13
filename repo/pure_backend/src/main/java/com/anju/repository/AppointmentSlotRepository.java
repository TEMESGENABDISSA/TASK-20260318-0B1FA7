package com.anju.repository;

import com.anju.entity.AppointmentSlot;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
    List<AppointmentSlot> findByStaffIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            Long staffId, LocalDateTime from, LocalDateTime to
    );

    boolean existsByStaffIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long staffId, String status, LocalDateTime endExclusive, LocalDateTime startExclusive
    );

    boolean existsByResourceIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long resourceId, String status, LocalDateTime endExclusive, LocalDateTime startExclusive
    );
}
