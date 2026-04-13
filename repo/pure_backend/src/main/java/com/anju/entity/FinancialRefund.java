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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_refund")
public class FinancialRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_no", nullable = false, unique = true, length = 64)
    private String refundNo;

    @Column(name = "transaction_no", nullable = false, length = 64)
    private String transactionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_mode", nullable = false, length = 32)
    private RefundMode refundMode;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason", nullable = false, length = 256)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
