package com.anju.service;

import com.anju.common.BusinessException;
import com.anju.entity.PaymentChannel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ImportExportValidationService {

    public Map<String, Object> validateTransactionRows(List<Map<String, Object>> rows) {
        if (rows == null) {
            throw new BusinessException("rows is required");
        }
        int success = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String err = validateRow(row);
            if (err == null) {
                success++;
            } else {
                errors.add(Map.of("rowIndex", i, "error", err));
            }
        }
        return Map.of(
                "total", rows.size(),
                "successCount", success,
                "failureCount", rows.size() - success,
                "errors", errors
        );
    }

    public Map<String, Object> validatePropertyRows(List<Map<String, Object>> rows) {
        return validateRows(rows, this::validatePropertyRow);
    }

    public Map<String, Object> validateAppointmentRows(List<Map<String, Object>> rows) {
        return validateRows(rows, this::validateAppointmentRow);
    }

    public Map<String, Object> fieldMappings() {
        Map<String, Object> mappings = new LinkedHashMap<>();
        mappings.put("transaction", List.of("transactionNo", "channel", "amount", "occurredAt"));
        mappings.put("property", List.of("propertyCode", "propertyName", "rent", "deposit", "rentalStartDate", "rentalEndDate"));
        mappings.put("appointment", List.of("appointmentNo", "propertyId", "staffId", "resourceId", "startTime", "durationMinutes"));
        return Map.of("mappings", mappings);
    }

    public Map<String, Object> validateFieldMapping(String domain, Map<String, String> mapping) {
        if (domain == null || mapping == null) {
            throw new BusinessException("domain and mapping are required");
        }
        List<String> expected = switch (domain) {
            case "transaction" -> List.of("transactionNo", "channel", "amount", "occurredAt");
            case "property" -> List.of("propertyCode", "propertyName", "rent", "deposit", "rentalStartDate", "rentalEndDate");
            case "appointment" -> List.of("appointmentNo", "propertyId", "staffId", "resourceId", "startTime", "durationMinutes");
            default -> throw new BusinessException("unsupported domain for mapping");
        };
        List<String> missing = expected.stream().filter(k -> !mapping.containsKey(k) || asString(mapping.get(k)).isBlank()).toList();
        return Map.of("domain", domain, "valid", missing.isEmpty(), "missingFields", missing);
    }

    private String validateRow(Map<String, Object> row) {
        if (row == null) {
            return "row is null";
        }
        String transactionNo = asString(row.get("transactionNo"));
        if (transactionNo == null || transactionNo.isBlank() || transactionNo.length() > 64) {
            return "transactionNo required and max length 64";
        }
        String channel = asString(row.get("channel"));
        try {
            PaymentChannel.valueOf(channel);
        } catch (Exception ex) {
            return "channel must be valid enum";
        }
        try {
            BigDecimal amount = new BigDecimal(String.valueOf(row.get("amount")));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return "amount must be positive";
            }
        } catch (Exception ex) {
            return "amount must be decimal";
        }
        try {
            LocalDateTime.parse(asString(row.get("occurredAt")));
        } catch (DateTimeParseException ex) {
            return "occurredAt must be ISO datetime";
        }
        return null;
    }

    private String validatePropertyRow(Map<String, Object> row) {
        if (row == null) {
            return "row is null";
        }
        String propertyCode = asString(row.get("propertyCode"));
        if (propertyCode == null || propertyCode.isBlank() || propertyCode.length() > 64) {
            return "propertyCode required and max length 64";
        }
        try {
            new BigDecimal(String.valueOf(row.get("rent")));
            new BigDecimal(String.valueOf(row.get("deposit")));
        } catch (Exception ex) {
            return "rent/deposit must be decimal";
        }
        try {
            LocalDate.parse(asString(row.get("rentalStartDate")));
            LocalDate.parse(asString(row.get("rentalEndDate")));
        } catch (Exception ex) {
            return "rentalStartDate/rentalEndDate must be date format yyyy-MM-dd";
        }
        return null;
    }

    private String validateAppointmentRow(Map<String, Object> row) {
        if (row == null) {
            return "row is null";
        }
        String appointmentNo = asString(row.get("appointmentNo"));
        if (appointmentNo == null || appointmentNo.isBlank() || appointmentNo.length() > 64) {
            return "appointmentNo required and max length 64";
        }
        try {
            int duration = Integer.parseInt(String.valueOf(row.get("durationMinutes")));
            if (!(duration == 15 || duration == 30 || duration == 60 || duration == 90)) {
                return "durationMinutes must be 15/30/60/90";
            }
        } catch (Exception ex) {
            return "durationMinutes must be integer";
        }
        try {
            LocalDateTime.parse(asString(row.get("startTime")));
        } catch (Exception ex) {
            return "startTime must be ISO datetime";
        }
        return null;
    }

    private Map<String, Object> validateRows(List<Map<String, Object>> rows, java.util.function.Function<Map<String, Object>, String> validator) {
        if (rows == null) {
            throw new BusinessException("rows is required");
        }
        int success = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String err = validator.apply(rows.get(i));
            if (err == null) {
                success++;
            } else {
                errors.add(Map.of("rowIndex", i, "error", err));
            }
        }
        return Map.of(
                "total", rows.size(),
                "successCount", success,
                "failureCount", rows.size() - success,
                "errors", errors
        );
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
