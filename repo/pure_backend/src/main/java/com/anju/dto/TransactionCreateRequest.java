package com.anju.dto;

import com.anju.entity.PaymentChannel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionCreateRequest {
    private String transactionNo;
    private Long appointmentId;
    private PaymentChannel channel;
    private BigDecimal amount;
    private boolean refundable;
    private LocalDateTime occurredAt;
}
