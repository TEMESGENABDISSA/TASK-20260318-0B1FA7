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
@Table(name = "financial_transaction")
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_no", nullable = false, unique = true, length = 64)
    private String transactionNo;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private PaymentChannel channel;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "refundable", nullable = false)
    private boolean refundable;

    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "settled", nullable = false)
    private boolean settled;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
