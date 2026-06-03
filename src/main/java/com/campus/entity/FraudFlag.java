package com.campus.entity;

import com.campus.enums.TransactionStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "flaggedAt"})
@ToString(exclude = {"createdAt", "flaggedAt"})
public class FraudFlag {
    private Long flagId;
    private Long studentId;
    private String reason;
    private Integer suspiciousTransactionCount;
    private TransactionStatus status;
    private String reviewNotes;
    private LocalDateTime createdAt;
    private LocalDateTime flaggedAt;
}
