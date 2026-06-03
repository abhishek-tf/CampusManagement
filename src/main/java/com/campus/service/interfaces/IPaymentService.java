package com.campus.service.interfaces;

import com.campus.entity.CampusPayment;
import com.campus.exception.CampusPaymentException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Campus Payments service contract (business layer).
 *
 * <p>WHAT: The operations the rest of the app may call to make and review campus payments.
 * WHY:  Depending on this interface (not the impl) follows the Dependency Inversion Principle
 *       and Interface Segregation — callers see only payment operations. It is intentionally
 *       small (two methods) rather than a god-interface.
 * HOW:  Methods declare {@code throws CampusPaymentException} so every business failure mode
 *       (invalid amount, student/wallet missing, insufficient balance, processing error) is a
 *       checked outcome the caller must handle.</p>
 */
public interface IPaymentService {

    /**
     * Processes a campus fee payment for a student: validates the student, wallet and amount,
     * creates a transaction + campus_payment, and debits the wallet — atomically.
     *
     * @param studentId the paying student's id (matches student.student_id)
     * @param category  the payment category (a {@link com.campus.enums.PaymentCategory} name)
     * @param amount    the amount to pay
     * @throws CampusPaymentException if validation fails or the payment cannot be processed
     */
    void processPayment(String studentId, String category, BigDecimal amount) throws CampusPaymentException;

    /**
     * Payment history for a student, most recent first.
     *
     * @throws CampusPaymentException if the student id is not usable
     */
    List<CampusPayment> getPaymentHistory(String studentId) throws CampusPaymentException;
}