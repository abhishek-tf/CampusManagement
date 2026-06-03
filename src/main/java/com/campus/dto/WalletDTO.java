package com.campus.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {
    private Long walletId;
    private Long studentId;
    private BigDecimal balance;
    private BigDecimal dailyTransferLimit;
    private LocalDateTime transferResetDate;
}
