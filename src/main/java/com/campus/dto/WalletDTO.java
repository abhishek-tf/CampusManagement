// Author: Hemanth
package com.campus.dto;

import com.campus.entity.Wallet;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-only view of a wallet handed to the presentation (menu) layer.
 *
 * WHY a DTO instead of returning the entity:
 *   - It decouples what the UI sees from the persistence model, so internal
 *     fields (e.g. updatedAt) are not leaked and the entity can change freely.
 *   - It is immutable (only getters) — the menu cannot accidentally mutate
 *     wallet state; all changes must go through the service layer.
 *
 * WHAT it exposes: the fields a user actually needs to see — balance, the
 *   per-day limit and how much of it is used, and the overall balance cap.
 *
 * HOW it is built: {@link #from(Wallet)} maps an entity to a DTO. Keeping the
 *   mapping here (one place) honours DRY — the service never hand-copies fields.
 */
@Getter
@Builder
@ToString
public class WalletDTO {

    private final Long walletId;
    private final Long studentId;
    private final BigDecimal balance;
    private final BigDecimal dailyTransferUsed;
    private final BigDecimal dailyTransferLimit;
    private final BigDecimal maxBalanceCap;
    private final LocalDate transferResetDate;

    /**
     * Maps a persistence {@link Wallet} entity to an immutable transfer object.
     *
     * @param wallet the source entity (must not be null)
     * @return a populated WalletDTO
     */
    public static WalletDTO from(Wallet wallet) {
        return WalletDTO.builder()
                .walletId(wallet.getWalletId())
                .studentId(wallet.getStudentId())
                .balance(wallet.getBalance())
                .dailyTransferUsed(wallet.getDailyTransferUsed())
                .dailyTransferLimit(wallet.getDailyTransferLimit())
                .maxBalanceCap(wallet.getMaxBalanceCap())
                .transferResetDate(wallet.getTransferResetDate())
                .build();
    }
}
