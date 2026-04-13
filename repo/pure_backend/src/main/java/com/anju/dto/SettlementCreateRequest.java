package com.anju.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettlementCreateRequest {
    private String settlementNo;
    private BigDecimal totalAmount;
}
