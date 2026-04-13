package com.anju.controller;

import com.anju.common.ApiResponse;
import com.anju.dto.AppointmentCreateRequest;
import com.anju.dto.AppointmentRescheduleRequest;
import com.anju.entity.Appointment;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import com.anju.service.AppointmentService;
import com.anju.service.IdempotencyService;
import com.anju.service.ImportExportValidationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final IdempotencyService idempotencyService;
    private final ImportExportValidationService importExportValidationService;

    public AppointmentController(AppointmentService appointmentService,
                                 IdempotencyService idempotencyService,
                                 ImportExportValidationService importExportValidationService) {
        this.appointmentService = appointmentService;
        this.idempotencyService = idempotencyService;
        this.importExportValidationService = importExportValidationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ApiResponse<Appointment> create(@RequestBody AppointmentCreateRequest request,
                                           @RequestHeader("Idempotency-Key") String idempotencyKey) {
        idempotencyService.verifyOrStore("appointment:create", idempotencyKey, String.valueOf(request.hashCode()));
        return ApiResponse.ok(appointmentService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','REVIEWER','ADMIN')")
    public ApiResponse<List<Appointment>> list() {
        return ApiResponse.ok(appointmentService.list());
    }

    @GetMapping("/{appointmentNo}")
    @PreAuthorize("hasAnyRole('DISPATCHER','REVIEWER','ADMIN')")
    public ApiResponse<Appointment> get(@PathVariable String appointmentNo) {
        return ApiResponse.ok(appointmentService.getByNo(appointmentNo));
    }

    @PostMapping("/{appointmentNo}:confirm")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ApiResponse<Appointment> confirm(@PathVariable String appointmentNo) {
        return ApiResponse.ok(appointmentService.confirm(appointmentNo));
    }

    @PostMapping("/{appointmentNo}:start-service")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ApiResponse<Appointment> startService(@PathVariable String appointmentNo) {
        return ApiResponse.ok(appointmentService.startService(appointmentNo));
    }

    @PostMapping("/{appointmentNo}:reschedule")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ApiResponse<Appointment> reschedule(@PathVariable String appointmentNo,
                                               @RequestBody AppointmentRescheduleRequest request,
                                               @RequestHeader("Idempotency-Key") String idempotencyKey) {
        idempotencyService.verifyOrStore("appointment:reschedule:" + appointmentNo, idempotencyKey, String.valueOf(request.hashCode()));
        return ApiResponse.ok(appointmentService.reschedule(appointmentNo, request));
    }

    @PostMapping("/{appointmentNo}:cancel")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ApiResponse<Appointment> cancel(@PathVariable String appointmentNo) {
        return ApiResponse.ok(appointmentService.cancel(appointmentNo));
    }

    @PostMapping("/{appointmentNo}:complete")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ApiResponse<Appointment> complete(@PathVariable String appointmentNo) {
        return ApiResponse.ok(appointmentService.complete(appointmentNo));
    }

    @PostMapping("/imports:validate")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ApiResponse<Map<String, Object>> validateAppointmentImport(
            @RequestBody List<Map<String, Object>> rows,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        idempotencyService.verifyOrStore("appointment:import:validate", idempotencyKey, String.valueOf(rows.hashCode()));
        return ApiResponse.ok(importExportValidationService.validateAppointmentRows(rows));
    }
}
