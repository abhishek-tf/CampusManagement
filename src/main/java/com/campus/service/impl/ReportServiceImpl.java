package com.campus.service.impl;

import com.campus.dto.report.DepartmentSpendReport;
import com.campus.dto.report.MonthlySummary;
import com.campus.dto.report.SpenderReport;
import com.campus.repository.interfaces.IReportRepository;
import com.campus.service.interfaces.IReportService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Reporting business logic. The heavy lifting (joins, grouping, summing) is done
 * by the database through {@link IReportRepository}; this layer validates inputs
 * and shields callers from the data source.
 */
public class ReportServiceImpl implements IReportService {

    private final IReportRepository reportRepository;

    public ReportServiceImpl(IReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public BigDecimal getTotalSpend() {
        return reportRepository.totalSpend();
    }

    @Override
    public List<SpenderReport> getTopSpenders(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return reportRepository.topSpenders(limit);
    }

    @Override
    public List<DepartmentSpendReport> getDepartmentWiseSpend() {
        return reportRepository.departmentWiseSpend();
    }

    @Override
    public List<MonthlySummary> getMonthlySummaries() {
        return reportRepository.monthlySummaries();
    }
}
