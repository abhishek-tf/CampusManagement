package com.campus.service.impl;

import com.campus.entity.CampusPayment;
import com.campus.enums.PaymentCategory;
import com.campus.enums.TransactionStatus;
import com.campus.exception.CampusPaymentException;
import com.campus.exception.InvalidAmountException;
import com.campus.repository.interfaces.IPaymentRepository;
import com.campus.service.interfaces.IPaymentService;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentServiceImpl implements IPaymentService {
    private final IPaymentRepository paymentRepository;

    public PaymentServiceImpl(IPaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void processPayment(Long studentId, String category, BigDecimal amount) throws CampusPaymentException {
        if (studentId == null || category == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Invalid payment data");
        }

        PaymentCategory paymentCategory = PaymentCategory.valueOf(category);

        CampusPayment payment = CampusPayment.builder()
                .studentId(studentId)
                .category(paymentCategory)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
    }
}
