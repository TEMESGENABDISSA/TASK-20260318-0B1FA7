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
@Table(name = "invoice_record")
public class InvoiceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_id", nullable = false)
    private Long statementId;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Column(name = "tax_no", nullable = false, length = 64)
    @Convert(converter = AesAttributeConverter.class)
    @JsonIgnore
    private String taxNo;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "invoice_no", length = 64)
    private String invoiceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvoiceStatus status;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @JsonProperty("taxNoMasked")
    public String getTaxNoMasked() {
        if (taxNo == null || taxNo.length() < 4) {
            return "****";
        }
        return "****" + taxNo.substring(taxNo.length() - 4);
    }
}
