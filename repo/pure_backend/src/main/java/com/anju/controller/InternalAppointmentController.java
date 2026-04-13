package com.anju.controller;

import com.anju.common.ApiResponse;
import com.anju.service.AppointmentService;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalAppointmentController {

    private final AppointmentService appointmentService;

    public InternalAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/appointments:release-expired")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Map<String, Object>> releaseExpired(@RequestParam(required = false) String beforeTime) {
        LocalDateTime t = (beforeTime == null || beforeTime.isBlank()) ? LocalDateTime.now() : LocalDateTime.parse(beforeTime);
        int released = appointmentService.releaseExpiredPendingBefore(t);
        return ApiResponse.ok(Map.of("released", released, "beforeTime", t.toString()));
    }
}
