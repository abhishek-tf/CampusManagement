package com.campus.service.impl;

import com.campus.dto.report.DepartmentSpendReport;
import com.campus.dto.report.MonthlySummary;
import com.campus.dto.report.SpendRecord;
import com.campus.dto.report.SpenderReport;
import com.campus.repository.interfaces.IReportRepository;
import com.campus.service.interfaces.IReportService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reporting business logic. The repository joins transaction -> wallet -> student
 * and returns raw spend rows; this layer aggregates them with Java Streams
 * (grouping, summing, ranking) to produce the reports.
 */
public class ReportServiceImpl implements IReportService {

    private final IReportRepository reportRepository;

    public ReportServiceImpl(IReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public BigDecimal getTotalSpend() {
        return sum(reportRepository.findSpendRecords());
    }

    @Override
    public List<SpenderReport> getTopSpenders(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Map<String, List<SpendRecord>> byStudent = reportRepository.findSpendRecords().stream()
                .collect(Collectors.groupingBy(SpendRecord::getStudentId));

        return byStudent.values().stream()
                .map(this::toSpenderReport)
                .sorted(Comparator.comparing(SpenderReport::getTotalSpent).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<DepartmentSpendReport> getDepartmentWiseSpend() {
        Map<String, List<SpendRecord>> byDepartment = reportRepository.findSpendRecords().stream()
                .collect(Collectors.groupingBy(SpendRecord::getDepartment));

        return byDepartment.entrySet().stream()
                .map(e -> new DepartmentSpendReport(e.getKey(), sum(e.getValue()), e.getValue().size()))
                .sorted(Comparator.comparing(DepartmentSpendReport::getTotalSpent).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<MonthlySummary> getMonthlySummaries() {
        Map<YearMonth, List<SpendRecord>> byMonth = reportRepository.findSpendRecords().stream()
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.groupingBy(r -> YearMonth.from(r.getCreatedAt())));

        return byMonth.entrySet().stream()
                .map(e -> new MonthlySummary(
                        e.getKey().getYear(),
                        e.getKey().getMonthValue(),
                        sum(e.getValue()),
                        e.getValue().size()))
                .sorted(Comparator.comparing(MonthlySummary::getYear)
                        .thenComparing(MonthlySummary::getMonth)
                        .reversed())
                .collect(Collectors.toList());
    }

    // ---- private Stream helpers ----

    private SpenderReport toSpenderReport(List<SpendRecord> records) {
        SpendRecord any = records.get(0);
        return new SpenderReport(
                any.getStudentId(),
                any.getStudentName(),
                any.getDepartment(),
                sum(records),
                records.size());
    }

    private BigDecimal sum(List<SpendRecord> records) {
        return records.stream()
                .map(SpendRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
