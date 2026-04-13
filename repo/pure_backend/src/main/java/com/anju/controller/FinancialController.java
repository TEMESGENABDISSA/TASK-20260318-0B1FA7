package com.anju.controller;

import com.anju.common.ApiResponse;
import com.anju.dto.InvoiceRequestCreate;
import com.anju.dto.RefundCreateRequest;
import com.anju.dto.SettlementCreateRequest;
import com.anju.dto.TransactionCreateRequest;
import com.anju.entity.DailyStatement;
import com.anju.entity.FinancialRefund;
import com.anju.entity.FinancialTransaction;
import com.anju.entity.InvoiceRecord;
import com.anju.entity.SettlementRecord;
import com.anju.security.RequireSecondaryPassword;
import com.anju.service.FinancialService;
import com.anju.service.IdempotencyService;
import com.anju.service.ImportExportValidationService;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/finance")
public class FinancialController {

    private final FinancialService financialService;
    private final IdempotencyService idempotencyService;
    private final ImportExportValidationService importExportValidationService;

    public FinancialController(FinancialService financialService,
                               IdempotencyService idempotencyService,
                               ImportExportValidationService importExportValidationService) {
        this.financialService = financialService;
        this.idempotencyService = idempotencyService;
        this.importExportValidationService = importExportValidationService;
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<FinancialTransaction> createTransaction(@RequestBody TransactionCreateRequest request,
                                                               @RequestHeader("Idempotency-Key") String idempotencyKey) {
        idempotencyService.verifyOrStore("finance:tx:create", idempotencyKey, String.valueOf(request.hashCode()));
        return ApiResponse.ok(financialService.recordTransaction(request));
    }

    @GetMapping("/transactions/{transactionNo}")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<FinancialTransaction> getTransaction(@PathVariable String transactionNo) {
        return ApiResponse.ok(financialService.getTransaction(transactionNo));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<List<FinancialTransaction>> listTransactions(@RequestParam(required = false) String from,
                                                                    @RequestParam(required = false) String to) {
        LocalDate fromDate = from == null || from.isBlank() ? null : LocalDate.parse(from);
        LocalDate toDate = to == null || to.isBlank() ? null : LocalDate.parse(to);
        return ApiResponse.ok(financialService.listTransactions(fromDate, toDate));
    }

    @PostMapping("/refunds")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    @RequireSecondaryPassword
    public ApiResponse<FinancialRefund> createRefund(@RequestBody RefundCreateRequest request,
                                                     @RequestHeader("Idempotency-Key") String idempotencyKey) {
        idempotencyService.verifyOrStore("finance:refund:create", idempotencyKey, String.valueOf(request.hashCode()));
        return ApiResponse.ok(financialService.createRefund(request));
    }

    @PostMapping("/statements:generate")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<DailyStatement> generateStatement(@RequestParam(required = false) String date) {
        LocalDate target = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        return ApiResponse.ok(financialService.generateDailyStatement(target));
    }

    @GetMapping("/statements")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<List<DailyStatement>> listStatements() {
        return ApiResponse.ok(financialService.listStatements());
    }

    @PostMapping("/invoices/requests")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<InvoiceRecord> createInvoiceRequest(@RequestBody InvoiceRequestCreate request) {
        return ApiResponse.ok(financialService.createInvoiceRequest(request));
    }

    @PostMapping("/invoices/{invoiceId}:mark-issued")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<InvoiceRecord> markInvoiceIssued(@PathVariable Long invoiceId, @RequestParam String invoiceNo) {
        return ApiResponse.ok(financialService.markInvoiceIssued(invoiceId, invoiceNo));
    }

    @PostMapping("/statements/{statementId}:export")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<Map<String, Object>> exportStatement(@PathVariable Long statementId, @RequestParam(defaultValue = "CSV") String format) {
        return ApiResponse.ok(Map.of(
                "statementId", statementId,
                "format", format,
                "status", "EXPORTED"
        ));
    }

    @GetMapping("/statements/{statementId}/export.csv")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ResponseEntity<byte[]> exportStatementCsv(@PathVariable Long statementId) {
        String csv = financialService.exportDailyStatementCsv(statementId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header("Content-Disposition", "attachment; filename=\"statement-" + statementId + ".csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/settlements")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<SettlementRecord> createSettlement(@RequestBody SettlementCreateRequest request) {
        return ApiResponse.ok(financialService.createSettlement(request.getSettlementNo(), request.getTotalAmount()));
    }

    @GetMapping("/settlements")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<List<SettlementRecord>> listSettlements() {
        return ApiResponse.ok(financialService.listSettlements());
    }

    @PostMapping("/imports/transactions:validate")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<Map<String, Object>> validateTransactionImport(
            @RequestBody List<Map<String, Object>> rows,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        idempotencyService.verifyOrStore("finance:import:validate", idempotencyKey, String.valueOf(rows.hashCode()));
        return ApiResponse.ok(importExportValidationService.validateTransactionRows(rows));
    }

    @GetMapping("/imports/field-mappings")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<Map<String, Object>> getFieldMappings() {
        return ApiResponse.ok(importExportValidationService.fieldMappings());
    }

    @PostMapping("/imports/{domain}:validate-mapping")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ApiResponse<Map<String, Object>> validateFieldMapping(@PathVariable String domain,
                                                                 @RequestBody Map<String, String> mapping,
                                                                 @RequestHeader("Idempotency-Key") String idempotencyKey) {
        idempotencyService.verifyOrStore("import:mapping:validate:" + domain, idempotencyKey, String.valueOf(mapping.hashCode()));
        return ApiResponse.ok(importExportValidationService.validateFieldMapping(domain, mapping));
    }
}
