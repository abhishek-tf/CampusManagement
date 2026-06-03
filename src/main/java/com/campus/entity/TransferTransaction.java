package com.campus.entity;

import com.campus.enums.TransactionStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@ToString(exclude = {"createdAt", "updatedAt"})
public class TransferTransaction {
    private Long transferId;
    private Long fromStudentId;
    private Long toStudentId;
    private BigDecimal amount;
    private TransactionStatus status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
