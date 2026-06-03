package com.campus.service.interfaces;

import com.campus.entity.Wallet;
import com.campus.exception.CampusPaymentException;

import java.math.BigDecimal;
import java.sql.Connection;

/**
 * Models the act of <em>processing</em> a campus payment for one category.
 *
 * <p>WHAT: A single-method functional interface representing payment-execution behaviour.
 *
 * WHY @FunctionalInterface: it declares exactly one abstract method, so the compiler enforces
 *      that contract and the strategy can be supplied as a lambda. This is how the requirement
 *      "use functional interfaces to model payment processing behaviour" is satisfied — the
 *      interface performs the work (create transaction + campus_payment, debit wallet), not
 *      merely a yes/no check.
 *
 * WHY an EnumMap of these (in the service) instead of a switch: the per-category strategies
 *      are registered once in an EnumMap<PaymentCategory, PaymentProcessor>; dispatching is a
 *      single map lookup. This is open for extension (add a category -> register a lambda) and
 *      avoids a growing switch-case that must be edited for every new category. EnumMap is the
 *      most efficient Map for enum keys (backed by an array indexed by ordinal).
 *
 * HOW: the method receives the active JDBC connection so it can participate in the caller's
 *      open transaction (it does NOT open or commit its own), keeping the whole payment atomic.</p>
 */
@FunctionalInterface
public interface PaymentProcessor {

    /**
     * Executes the payment within the caller's already-open transaction.
     *
     * @param connection the active JDBC connection (transaction in progress, autocommit off)
     * @param wallet     the locked wallet to debit
     * @param amount     the validated payment amount
     * @return the generated transaction id (so the caller can trace/audit the payment)
     * @throws CampusPaymentException if the payment cannot be processed
     */
    long process(Connection connection, Wallet wallet, BigDecimal amount) throws CampusPaymentException;
}
