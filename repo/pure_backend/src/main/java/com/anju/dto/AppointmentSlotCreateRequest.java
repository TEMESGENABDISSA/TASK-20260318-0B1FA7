package com.anju.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentSlotCreateRequest {
    private Long staffId;
    private Long resourceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
