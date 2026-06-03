package com.campus.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;


public class WalletDTO {
    private Long walletId;
    private Long studentId;
    private BigDecimal balance;
    private BigDecimal dailyTransferLimit;
    private LocalDateTime transferResetDate;
}
