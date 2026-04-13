package com.anju.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyResponse {
    private Long id;
    private String propertyCode;
    private String propertyName;
    private BigDecimal rent;
    private BigDecimal deposit;
    private String status;
    private LocalDate rentalStartDate;
    private LocalDate rentalEndDate;
    private List<Long> materialFileIds = new ArrayList<>();
    private List<VacancyPeriodDto> vacancyPeriods = new ArrayList<>();
}
