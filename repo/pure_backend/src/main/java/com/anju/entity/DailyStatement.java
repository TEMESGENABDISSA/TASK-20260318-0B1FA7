package com.anju.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "daily_statement")
public class DailyStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_date", nullable = false, unique = true)
    private LocalDate statementDate;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    @Column(name = "has_exception", nullable = false)
    private boolean hasException;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private StatementStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
