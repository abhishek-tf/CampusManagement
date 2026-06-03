package com.campus.service.interfaces;

import com.campus.entity.CampusPayment;
import com.campus.exception.CampusPaymentException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Business contract for campus payments (canteen, library fine, hackathon fee,
 * workshop fee, hostel fee). A payment debits the student's wallet and records
 * both a {@code transaction} and a {@code campus_payment} row atomically.
 */
public interface IPaymentService {

    /**
     * Pays a campus fee from the student's wallet.
     *
     * @param studentId the paying student
     * @param category  one of the {@link com.campus.enums.PaymentCategory} names
     * @param amount    positive amount to pay
     * @throws CampusPaymentException if the input is invalid, the category is
     *         unknown, the wallet is missing, or the balance is insufficient
     */
    void processPayment(String studentId, String category, BigDecimal amount) throws CampusPaymentException;

    /** @return the student's payment history (most recent first). */
    List<CampusPayment> getPaymentHistory(String studentId) throws CampusPaymentException;
}
