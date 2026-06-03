package com.campus.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DepartmentSpendReport {
    private String department;
    private BigDecimal totalSpent;
    private long transactionCount;
}
