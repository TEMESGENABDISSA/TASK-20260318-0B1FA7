package com.anju.controller;

import com.anju.common.ApiResponse;
import com.anju.security.RequireSecondaryPassword;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance/admin")
public class FinanceAdminController {

    @PostMapping("/refunds/{transactionNo}/approve")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    @RequireSecondaryPassword
    public ApiResponse<Map<String, String>> approveRefund(@PathVariable String transactionNo) {
        return ApiResponse.ok(Map.of(
                "transactionNo", transactionNo,
                "status", "APPROVED"
        ));
    }
}
