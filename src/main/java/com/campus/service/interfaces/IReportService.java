package com.campus.service.interfaces;

import com.campus.dto.report.DepartmentSpendReport;
import com.campus.dto.report.MonthlySummary;
import com.campus.dto.report.SpenderReport;
import java.math.BigDecimal;
import java.util.List;

public interface IReportService {

    /** Total money spent across all SUCCESS spend transactions. */
    BigDecimal getTotalSpend();

    /** Students ranked by total amount spent, highest first. */
    List<SpenderReport> getTopSpenders(int limit);

    /** Spend grouped by student department, highest first. */
    List<DepartmentSpendReport> getDepartmentWiseSpend();

    /** Spend grouped by calendar month, most recent first. */
    List<MonthlySummary> getMonthlySummaries();
}
