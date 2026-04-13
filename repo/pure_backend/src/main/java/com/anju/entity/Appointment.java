package com.anju.entity;

import com.anju.config.AesAttributeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Convert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_no", nullable = false, unique = true, length = 64)
    private String appointmentNo;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AppointmentStatus status;

    @Column(name = "service_type", nullable = false, length = 64)
    private String serviceType;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "order_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "penalty_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "reschedule_count", nullable = false)
    private Integer rescheduleCount;

    @Column(name = "confirm_deadline", nullable = false)
    private LocalDateTime confirmDeadline;

    @JsonIgnore
    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "contact_phone", length = 255)
    private String contactPhone;

    @JsonIgnore
    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "id_no", length = 255)
    private String idNo;

    @JsonProperty("contactPhoneMasked")
    public String getContactPhoneMasked() {
        if (contactPhone == null || contactPhone.length() < 4) {
            return "****";
        }
        int keep = Math.min(4, contactPhone.length());
        return "****" + contactPhone.substring(contactPhone.length() - keep);
    }

    @JsonProperty("idNoMasked")
    public String getIdNoMasked() {
        if (idNo == null || idNo.length() < 4) {
            return "****";
        }
        int keep = Math.min(4, idNo.length());
        return "****" + idNo.substring(idNo.length() - keep);
    }
}
