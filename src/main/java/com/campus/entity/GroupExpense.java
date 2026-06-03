package com.campus.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.campus.enums.SplitType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps the {@code group_expense} table: a single bill paid by one group member.
 * {@code paidBy} is the payer's {@code student_id}; {@code splitType} records how
 * the matching {@code expense_split} rows were computed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupExpense {
    private Long expenseId;
    private Long groupId;
    private String paidBy;
    private String description;
    private BigDecimal totalAmount;
    private SplitType splitType;
    private LocalDateTime createdAt;
}
