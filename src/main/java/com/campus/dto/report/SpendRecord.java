package com.campus.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One spend transaction enriched with its student + department via SQL join.
 * The report service streams over these rows to build the aggregate reports.
 */
@Data
@AllArgsConstructor
public class SpendRecord {
    private String studentId;
    private String studentName;
    private String department;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
