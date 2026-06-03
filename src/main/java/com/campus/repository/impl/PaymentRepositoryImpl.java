package com.campus.repository.impl;

import com.campus.entity.CampusPayment;
import com.campus.repository.interfaces.IPaymentRepository;
import java.util.*;
import java.util.stream.Collectors;

public class PaymentRepositoryImpl implements IPaymentRepository {
    private final Map<Long, CampusPayment> store = new HashMap<>();
    private static long idCounter = 1;

    @Override
    public void save(CampusPayment payment) {
        if (payment.getPaymentId() == null) {
            payment.setPaymentId(idCounter++);
        }
        store.put(payment.getPaymentId(), payment);
    }

    @Override
    public Optional<CampusPayment> findById(Long paymentId) {
        return Optional.ofNullable(store.get(paymentId));
    }

    @Override
    public List<CampusPayment> findByStudentId(Long studentId) {
        return store.values().stream()
                .filter(p -> p.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    @Override
    public List<CampusPayment> findAll() {
        return new ArrayList<>(store.values());
    }
}
