package com.campus.repository.interfaces;

import com.campus.entity.CampusPayment;
import java.sql.Connection;
import java.util.Optional;
import java.util.List;

/**
 * Persistence contract for {@code campus_payment}.
 *
 * <p>WHAT: Pure data-access operations for campus payments.
 * WHY:  Defined as an interface so the service depends on an abstraction (DIP) and the JDBC
 *       implementation can be swapped/mocked. The repository exposes only SQL operations — no
 *       balance checks, no validation — because business rules live in the service layer
 *       (Single Responsibility): a repository that also validated would have two reasons to change.
 * HOW:  The write method accepts a {@link Connection} so the insert can enlist in the service's
 *       existing transaction rather than opening its own — essential for atomic payments.</p>
 */
public interface IPaymentRepository {

    Optional<CampusPayment> findById(Long paymentId);

    List<CampusPayment> findByStudentId(String studentId);

    List<CampusPayment> findAll();

    /**
     * Inserts a campus_payment row within the caller's JDBC transaction.
     * WHY a Connection param: keeps this insert part of the same atomic unit of work as the
     * transaction insert and wallet debit, so all three commit or roll back together.
     */
    void insert(Connection conn, CampusPayment payment);
}