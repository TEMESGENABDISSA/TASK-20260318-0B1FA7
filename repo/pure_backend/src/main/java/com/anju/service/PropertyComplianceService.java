package com.anju.service;

import com.anju.entity.Property;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PropertyComplianceService {

    public ComplianceResult evaluate(Property property) {
        List<String> violations = new ArrayList<>();
        if (property.getPropertyCode() == null || property.getPropertyCode().isBlank()) {
            violations.add("propertyCode missing");
        }
        if (property.getRent() == null || property.getRent().compareTo(BigDecimal.ZERO) < 0) {
            violations.add("rent must be >= 0");
        }
        if (property.getDeposit() == null || property.getDeposit().compareTo(BigDecimal.ZERO) < 0) {
            violations.add("deposit must be >= 0");
        }
        LocalDate start = property.getRentalStartDate();
        LocalDate end = property.getRentalEndDate();
        if (start == null || end == null || end.isBefore(start)) {
            violations.add("rental period invalid");
        }
        if (property.getPropertyName() == null || property.getPropertyName().isBlank() || property.getPropertyName().length() > 128) {
            violations.add("propertyName required and <= 128 chars");
        }
        return new ComplianceResult(violations.isEmpty(), violations);
    }

    public record ComplianceResult(boolean pass, List<String> violations) {}
}
