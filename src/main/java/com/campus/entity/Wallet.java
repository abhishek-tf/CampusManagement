package com.campus.entity;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@ToString(exclude = {"createdAt", "updatedAt"})
public class Wallet {
    private Long walletId;
    private Long studentId;
    private BigDecimal balance;
    private BigDecimal dailyTransferLimit;
    private BigDecimal dailyTransferSpent;
    private LocalDateTime transferResetDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
