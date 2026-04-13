package com.anju.service;

import com.anju.common.BusinessException;
import com.anju.dto.AppointmentSlotCreateRequest;
import com.anju.entity.AppointmentSlot;
import com.anju.repository.AppointmentSlotRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentSlotService {

    private final AppointmentSlotRepository appointmentSlotRepository;

    public AppointmentSlotService(AppointmentSlotRepository appointmentSlotRepository) {
        this.appointmentSlotRepository = appointmentSlotRepository;
    }

    @Transactional
    public AppointmentSlot create(AppointmentSlotCreateRequest request) {
        if (request.getStaffId() == null || request.getResourceId() == null) {
            throw new BusinessException("staffId and resourceId are required");
        }
        if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("invalid slot time range");
        }
        boolean staffOverlap = appointmentSlotRepository.existsByStaffIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                request.getStaffId(), "AVAILABLE", request.getEndTime(), request.getStartTime()
        );
        if (staffOverlap) {
            throw new BusinessException("slot overlap for staff");
        }
        boolean resourceOverlap = appointmentSlotRepository.existsByResourceIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                request.getResourceId(), "AVAILABLE", request.getEndTime(), request.getStartTime()
        );
        if (resourceOverlap) {
            throw new BusinessException("slot overlap for resource");
        }
        AppointmentSlot slot = new AppointmentSlot();
        slot.setStaffId(request.getStaffId());
        slot.setResourceId(request.getResourceId());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setStatus("AVAILABLE");
        return appointmentSlotRepository.save(slot);
    }

    @Transactional(readOnly = true)
    public List<AppointmentSlot> list(Long staffId, LocalDateTime from, LocalDateTime to) {
        if (staffId != null && from != null && to != null) {
            return appointmentSlotRepository.findByStaffIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(staffId, from, to);
        }
        return appointmentSlotRepository.findAll();
    }

    @Transactional
    public void invalidate(Long slotId) {
        AppointmentSlot slot = appointmentSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException("slot not found"));
        slot.setStatus("INVALIDATED");
        appointmentSlotRepository.save(slot);
    }
}
