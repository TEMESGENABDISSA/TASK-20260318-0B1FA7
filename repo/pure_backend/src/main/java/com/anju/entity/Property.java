package com.anju.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "property")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_code", nullable = false, unique = true, length = 64)
    private String propertyCode;

    @Column(name = "property_name", nullable = false, length = 128)
    private String propertyName;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "rent", nullable = false, precision = 18, scale = 2)
    private BigDecimal rent;

    @Column(name = "deposit", nullable = false, precision = 18, scale = 2)
    private BigDecimal deposit;

    @Column(name = "rental_start_date")
    private LocalDate rentalStartDate;

    @Column(name = "rental_end_date")
    private LocalDate rentalEndDate;

    @Column(name = "compliance_result", length = 32)
    private String complianceResult;
}
