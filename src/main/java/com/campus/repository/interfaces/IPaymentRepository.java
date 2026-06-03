package com.campus.repository.interfaces;

import com.campus.entity.CampusPayment;
import java.util.Optional;
import java.util.List;

public interface IPaymentRepository {
    void save(CampusPayment payment);
    Optional<CampusPayment> findById(Long paymentId);
    List<CampusPayment> findByStudentId(Long studentId);
    List<CampusPayment> findAll();
}
