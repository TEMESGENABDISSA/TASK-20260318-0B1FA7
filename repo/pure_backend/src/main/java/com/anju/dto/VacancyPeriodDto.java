package com.anju.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VacancyPeriodDto {
    private LocalDate startDate;
    private LocalDate endDate;
}
