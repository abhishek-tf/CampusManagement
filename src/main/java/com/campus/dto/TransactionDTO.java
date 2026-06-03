package com.campus.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Long transactionId;
    private Long studentId;
    private String type;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
