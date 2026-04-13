package com.anju.controller;

import com.anju.common.ApiResponse;
import com.anju.dto.PropertyCreateRequest;
import com.anju.dto.PropertyMaterialsRequest;
import com.anju.dto.PropertyResponse;
import com.anju.dto.PropertyReviewRequest;
import com.anju.dto.PropertyUpdateRequest;
import com.anju.security.RequireSecondaryPassword;
import com.anju.service.IdempotencyService;
import com.anju.service.ImportExportValidationService;
import com.anju.service.PropertyService;
import java.util.Map;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final PropertyService propertyService;
    private final IdempotencyService idempotencyService;
    private final ImportExportValidationService importExportValidationService;

    public PropertyController(PropertyService propertyService,
                              IdempotencyService idempotencyService,
                              ImportExportValidationService importExportValidationService) {
        this.propertyService = propertyService;
        this.idempotencyService = idempotencyService;
        this.importExportValidationService = importExportValidationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ApiResponse<PropertyResponse> create(@RequestBody PropertyCreateRequest request) {
        return ApiResponse.ok(propertyService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR','REVIEWER','DISPATCHER','FINANCE','ADMIN')")
    public ApiResponse<List<PropertyResponse>> list() {
        return ApiResponse.ok(propertyService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR','REVIEWER','DISPATCHER','FINANCE','ADMIN')")
    public ApiResponse<PropertyResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(propertyService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ApiResponse<PropertyResponse> update(@PathVariable Long id, @RequestBody PropertyUpdateRequest request) {
        return ApiResponse.ok(propertyService.update(id, request));
    }

    @PostMapping("/{id}/materials")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ApiResponse<PropertyResponse> attachMaterials(@PathVariable Long id, @RequestBody PropertyMaterialsRequest request) {
        return ApiResponse.ok(propertyService.attachMaterials(id, request));
    }

    @PostMapping("/{id}:submit-review")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ApiResponse<PropertyResponse> submitReview(@PathVariable Long id) {
        return ApiResponse.ok(propertyService.submitReview(id));
    }

    @PostMapping("/{id}:review")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public ApiResponse<PropertyResponse> review(@PathVariable Long id, @RequestBody PropertyReviewRequest request) {
        return ApiResponse.ok(propertyService.review(id, request));
    }

    @PostMapping("/{id}:invalidate")
    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @RequireSecondaryPassword
    public ApiResponse<PropertyResponse> invalidate(@PathVariable Long id) {
        return ApiResponse.ok(propertyService.invalidate(id));
    }

    @PostMapping("/imports:validate")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ApiResponse<Map<String, Object>> validatePropertyImport(
            @RequestBody List<Map<String, Object>> rows,
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey) {
        idempotencyService.verifyOrStore("property:import:validate", idempotencyKey, String.valueOf(rows.hashCode()));
        return ApiResponse.ok(importExportValidationService.validatePropertyRows(rows));
    }
}
