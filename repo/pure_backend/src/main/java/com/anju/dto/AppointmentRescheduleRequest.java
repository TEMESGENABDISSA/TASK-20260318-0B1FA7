package com.anju.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentRescheduleRequest {
    private LocalDateTime newStartTime;
    private Integer durationMinutes;
}
