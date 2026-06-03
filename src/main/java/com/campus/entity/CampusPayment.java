package com.campus.entity;

import com.campus.enums.PaymentCategory;
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
public class CampusPayment {
    private Long paymentId;
    private Long studentId;
    private PaymentCategory category;
    private BigDecimal amount;
    private TransactionStatus status;
    private String invoiceNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
