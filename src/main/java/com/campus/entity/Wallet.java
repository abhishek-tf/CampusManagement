// Author: Hemanth
package com.campus.entity;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain entity mapping the {@code wallet} table (see schema.sql).
 *
 * WHY each non-obvious field exists (schema is the source of truth — CLAUDE.md):
 *   - dailyTransferUsed + transferResetDate: together they let the app reset the
 *     daily transfer counter once per calendar DAY without scanning the
 *     transaction table. transferResetDate is a DATE, so it is modelled as
 *     {@link LocalDate} (not LocalDateTime) to mirror the column exactly.
 *   - maxBalanceCap / dailyTransferLimit: per-wallet limits enforced by the
 *     service layer. They live on the row (not as global constants) so each
 *     wallet can carry its own cap.
 *
 * WHAT this class is: a pure data holder (fields + Lombok-generated
 *   constructors/getters/setters/toString). Per CLAUDE.md, entities contain NO
 *   business logic — all rules live in WalletServiceImpl.
 *
 * NOTE on money: every monetary field uses {@link BigDecimal} (never double) to
 *   avoid binary floating-point rounding errors on currency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"updatedAt"})
@ToString(exclude = {"updatedAt"})
public class Wallet {

    /** wallet_id BIGINT PK (DB-generated). */
    private Long walletId;

    /** student_id — owner of the wallet; one wallet per student (UNIQUE in schema). */
    private Long studentId;

    /** balance DECIMAL(12,2) — current spendable amount. */
    private BigDecimal balance;

    /** daily_transfer_used DECIMAL(12,2) — amount transferred so far on transferResetDate. */
    private BigDecimal dailyTransferUsed;

    /** transfer_reset_date DATE — the calendar day dailyTransferUsed is counted against. */
    private LocalDate transferResetDate;

    /** max_balance_cap DECIMAL(12,2) — balance may never exceed this. */
    private BigDecimal maxBalanceCap;

    /** daily_transfer_limit DECIMAL(12,2) — max total transfers allowed per day. */
    private BigDecimal dailyTransferLimit;

    /** updated_at DATETIME — last modification (DB maintains ON UPDATE CURRENT_TIMESTAMP). */
    private LocalDateTime updatedAt;
}
