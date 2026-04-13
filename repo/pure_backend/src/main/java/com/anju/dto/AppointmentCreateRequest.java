package com.anju.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentCreateRequest {
    private String appointmentNo;
    private Long propertyId;
    private String serviceType;
    private Integer durationMinutes;
    private Long staffId;
    private Long resourceId;
    private LocalDateTime startTime;
    private BigDecimal orderAmount;
    private String contactPhone;
    private String idNo;
}
