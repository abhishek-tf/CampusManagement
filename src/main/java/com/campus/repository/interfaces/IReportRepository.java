package com.campus.repository.interfaces;

import com.campus.dto.report.DepartmentSpendReport;
import com.campus.dto.report.MonthlySummary;
import com.campus.dto.report.SpenderReport;
import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only reporting queries against the transaction data.
 *
 * <p>Each method runs a single SQL aggregation (with joins to wallet/student
 * where the report needs student or department context). The database does the
 * grouping and summing; the service layer only orchestrates and validates.</p>
 */
public interface IReportRepository {

    /** Total amount of all SUCCESS spend transactions. */
    BigDecimal totalSpend();

    /** Students ranked by total spend, highest first. */
    List<SpenderReport> topSpenders(int limit);

    /** Spend grouped by student department, highest first. */
    List<DepartmentSpendReport> departmentWiseSpend();

    /** Spend grouped by calendar month, most recent first. */
    List<MonthlySummary> monthlySummaries();
}
