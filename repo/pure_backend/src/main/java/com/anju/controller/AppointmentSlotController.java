package com.anju.controller;

import com.anju.common.ApiResponse;
import com.anju.dto.AppointmentSlotCreateRequest;
import com.anju.entity.AppointmentSlot;
import com.anju.service.AppointmentSlotService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointment-slots")
public class AppointmentSlotController {

    private final AppointmentSlotService appointmentSlotService;

    public AppointmentSlotController(AppointmentSlotService appointmentSlotService) {
        this.appointmentSlotService = appointmentSlotService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ApiResponse<AppointmentSlot> create(@RequestBody AppointmentSlotCreateRequest request) {
        return ApiResponse.ok(appointmentSlotService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','REVIEWER','ADMIN')")
    public ApiResponse<List<AppointmentSlot>> list(@RequestParam(required = false) Long staffId,
                                                   @RequestParam(required = false) String from,
                                                   @RequestParam(required = false) String to) {
        LocalDateTime fromTime = from == null || from.isBlank() ? null : LocalDateTime.parse(from);
        LocalDateTime toTime = to == null || to.isBlank() ? null : LocalDateTime.parse(to);
        return ApiResponse.ok(appointmentSlotService.list(staffId, fromTime, toTime));
    }

    @PostMapping("/{slotId}:invalidate")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ApiResponse<String> invalidate(@PathVariable Long slotId) {
        appointmentSlotService.invalidate(slotId);
        return ApiResponse.ok("INVALIDATED");
    }
}
