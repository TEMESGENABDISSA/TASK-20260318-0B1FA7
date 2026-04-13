package com.anju.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "settlement_record")
public class SettlementRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_no", nullable = false, unique = true, length = 64)
    private String settlementNo;

    @Column(name = "settlement_date", nullable = false)
    private LocalDateTime settlementDate;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false, length = 32)
    private String status;
}
