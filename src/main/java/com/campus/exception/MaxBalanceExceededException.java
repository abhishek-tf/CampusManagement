// Author: Hemanth
package com.campus.exception;

/**
 * Thrown when a credit (top-up or incoming transfer) would push a wallet's
 * balance above its {@code max_balance_cap}.
 *
 * WHY: the schema enforces a per-wallet cap (max_balance_cap column). Rejecting
 *   early in the service gives the user a clear business error instead of a raw
 *   SQL/constraint failure surfacing from the database.
 * WHAT: message + stable error code "MAX_BALANCE_EXCEEDED".
 * HOW: WalletServiceImpl checks (currentBalance + credit) against the cap before
 *   it ever calls the repository.
 */
public class MaxBalanceExceededException extends CampusPaymentException {

    public MaxBalanceExceededException(String message) {
        super(message, "MAX_BALANCE_EXCEEDED");
    }
}
