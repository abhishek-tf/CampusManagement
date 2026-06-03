// Author: Hemanth
package com.campus.exception;

/**
 * Thrown when a wallet lookup (by student id or wallet id) finds no row.
 *
 * WHY a dedicated type: CLAUDE.md forbids throwing generic Exception for
 *   business cases. A distinct type lets the menu/service layer react
 *   specifically to a "missing wallet" situation (e.g. offer to create one)
 *   instead of guessing from a string message.
 * WHAT it carries: a human-readable message plus the stable error code
 *   "WALLET_NOT_FOUND" (set via the CampusPaymentException supertype).
 * HOW it is used: repositories return Optional; the service converts an empty
 *   Optional into this checked exception so callers are forced to handle it.
 */
public class WalletNotFoundException extends CampusPaymentException {

    public WalletNotFoundException(String message) {
        super(message, "WALLET_NOT_FOUND");
    }
}
