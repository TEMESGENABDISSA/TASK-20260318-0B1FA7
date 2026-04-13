package com.anju.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceRequestCreate {
    private Long statementId;
    private String title;
    private String taxNo;
    private BigDecimal amount;
}
