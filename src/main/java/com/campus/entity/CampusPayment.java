package com.campus.entity;

import com.campus.enums.PaymentCategory;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps the {@code campus_payment} table: the subtype detail for a PAYMENT
 * transaction. One-to-one with {@code transaction} via {@code txnId} (the schema
 * enforces UNIQUE txn_id). A pure data holder - no business logic (CLAUDE.md).
 *
 * Schema mapping (source of truth = schema.sql):
 *   payment_id BIGINT PK        -> paymentId  (DB-generated)
 *   txn_id     BIGINT UQ NOT NULL-> txnId      (FK to transaction)
 *   student_id VARCHAR(20)      -> studentId
 *   category   VARCHAR(30)      -> category
 *   amount     DECIMAL(12,2)    -> amount
 *   paid_at    DATETIME         -> paidAt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"paidAt"})
@ToString(exclude = {"paidAt"})
public class CampusPayment {
    private Long paymentId;
    private Long txnId;
    private String studentId;
    private PaymentCategory category;
    private BigDecimal amount;
    private LocalDateTime paidAt;
}
