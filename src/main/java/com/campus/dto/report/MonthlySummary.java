package com.campus.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MonthlySummary {
    private int year;
    private int month;
    private BigDecimal totalSpent;
    private long transactionCount;
}
