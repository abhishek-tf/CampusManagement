package com.campus.entity;

import com.campus.enums.TransactionType;
import com.campus.enums.TransactionStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@ToString(exclude = {"createdAt"})
public class TransactionHistory {
    private Long transactionId;
    private Long studentId;
    private Long walletId;
    private TransactionType txnType;
    private BigDecimal amount;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
}
