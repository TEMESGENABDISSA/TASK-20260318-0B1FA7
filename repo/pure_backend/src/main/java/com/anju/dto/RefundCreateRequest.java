package com.anju.dto;

import com.anju.entity.RefundMode;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundCreateRequest {
    private String refundNo;
    private String transactionNo;
    private RefundMode refundMode;
    private BigDecimal amount;
    private String reason;
}
