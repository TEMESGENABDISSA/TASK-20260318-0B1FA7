package com.anju.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyUpdateRequest {
    private String propertyName;
    private BigDecimal rent;
    private BigDecimal deposit;
    private LocalDate rentalStartDate;
    private LocalDate rentalEndDate;
    private List<VacancyPeriodDto> vacancyPeriods;
}
