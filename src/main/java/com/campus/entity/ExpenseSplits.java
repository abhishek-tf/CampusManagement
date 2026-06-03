package com.campus.entity;

import com.campus.enums.SettlementStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "settledAt"})
@ToString(exclude = {"createdAt", "settledAt"})
public class ExpenseSplits {
    private Long splitId;
    private Long groupId;
    private Long studentId;
    private BigDecimal shareAmount;
    private BigDecimal paidAmount;
    private SettlementStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;
}
