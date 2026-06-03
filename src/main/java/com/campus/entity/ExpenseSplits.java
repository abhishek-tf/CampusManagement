package com.campus.entity;

import com.campus.enums.SettlementStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps the {@code expense_split} table: one debtor's share of a single bill.
 * When settled, {@code status} becomes {@link SettlementStatus#SETTLED},
 * {@code settledTxnId} points at the wallet TRANSFER that paid it (null for the
 * payer's own auto-settled share) and {@code settledAt} is stamped.
 * {@code sharePercent} is populated only for {@code PERCENT} splits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSplits {
    private Long splitId;
    private Long expenseId;
    private String debtorId;
    private BigDecimal shareAmount;
    private BigDecimal sharePercent;
    private SettlementStatus status;
    private Long settledTxnId;
    private LocalDateTime settledAt;
}
