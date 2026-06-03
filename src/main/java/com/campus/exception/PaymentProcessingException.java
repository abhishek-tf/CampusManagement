package com.campus.exception;

/**
 * Raised when a campus payment cannot be processed: an unknown category, or a persistence
 * failure that forced the transaction to roll back.
 *
 * <p>WHAT: The "payment processing failure" business exception.
 * WHY:  Separates a genuine processing/infrastructure failure from input-validation failures
 *       (invalid amount) and balance failures (insufficient funds), so the menu/caller can
 *       react appropriately. Checked, because the caller must decide how to surface it.
 * HOW:  Two constructors — one for a plain business message (unknown category), one that
 *       wraps the underlying cause (e.g. a DataAccessException) so the root SQL error is not
 *       lost while still presenting a business-level type to callers.</p>
 */
public class PaymentProcessingException extends CampusPaymentException {

    public PaymentProcessingException(String message) {
        // WHY: Stable code for the "processing failed" family of errors.
        super(message, "PAYMENT_PROCESSING_FAILED");
    }

    public PaymentProcessingException(String message, Throwable cause) {
        // WHY: Preserve the original cause (e.g. SQL/data error) for diagnostics while the
        //      caller only needs to handle the business-level PaymentProcessingException.
        super(message, cause);
    }
}
