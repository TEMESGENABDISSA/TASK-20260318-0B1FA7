package com.anju.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "idempotency_record",
        indexes = {
                @Index(name = "idx_idempotency_expire_at", columnList = "expire_at")
        }
)
public class IdempotencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope", nullable = false, length = 100)
    private String scope;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "fingerprint", nullable = false, length = 128)
    private String fingerprint;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;
}
