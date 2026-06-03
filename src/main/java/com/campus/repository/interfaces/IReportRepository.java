package com.campus.repository.interfaces;

import com.campus.dto.report.SpendRecord;
import java.util.List;

/**
 * Read-only reporting data access.
 *
 * <p>The repository's job is the <em>join</em>: it fetches each SUCCESS spend
 * transaction together with its student and department. All grouping, summing
 * and ranking is done by the service layer using Java Streams.</p>
 */
public interface IReportRepository {

    /** Every SUCCESS spend transaction (PAYMENT/WITHDRAW/TRANSFER) joined to its student. */
    List<SpendRecord> findSpendRecords();
}
