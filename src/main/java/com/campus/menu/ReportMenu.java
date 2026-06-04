package com.campus.menu;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

import com.campus.entity.Transaction;
import com.campus.exception.CampusPaymentException;
import com.campus.service.interfaces.IReportService;
import com.campus.service.interfaces.ITransactionService;

/**
 * Console controller for Transaction History &amp; Reports. All I/O lives here;
 * recording/history delegate to {@link ITransactionService} and the Stream-based
 * reports to {@link IReportService} (layered architecture).
 */
public class ReportMenu {

    private final IReportService reportService;
    private final ITransactionService transactionService;
    private final Scanner scanner;

    public ReportMenu(IReportService reportService, ITransactionService transactionService, Scanner scanner) {
        this.reportService = reportService;
        this.transactionService = transactionService;
        this.scanner = scanner;
    }

    public void show() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Transaction History & Reports ---");
            System.out.println("1. Record Transaction");
            System.out.println("2. View Wallet Transaction History");
            System.out.println("3. Total Spend");
            System.out.println("4. Top Spenders");
            System.out.println("5. Department-wise Spend");
            System.out.println("6. Monthly Summary");
            System.out.println("7. Back");
            switch (prompt("Choice")) {
                case "1" -> recordTransaction();
                case "2" -> viewWalletHistory();
                case "3" -> System.out.println("\nTotal Spend: " + reportService.getTotalSpend());
                case "4" -> showTopSpenders();
                case "5" -> showDepartmentWiseSpend();
                case "6" -> showMonthlySummary();
                case "7" -> back = true;
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private void recordTransaction() {
        Long walletId = readLong("Wallet id");
        if (walletId == null) {
            return;
        }
        String type = prompt("Type (DEPOSIT/WITHDRAW/TRANSFER/PAYMENT)");
        String rawAmount = prompt("Amount");
        try {
            Transaction txn = transactionService.recordTransaction(walletId, type, new BigDecimal(rawAmount));
            System.out.println("Recorded transaction #" + txn.getTxnId()
                    + " (" + txn.getTxnType() + " " + txn.getAmount() + ")");
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount: " + rawAmount);
        } catch (CampusPaymentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewWalletHistory() {
        Long walletId = readLong("Wallet id");
        if (walletId == null) {
            return;
        }
        try {
            List<Transaction> history = transactionService.getWalletHistory(walletId);
            if (history.isEmpty()) {
                System.out.println("No transactions found for wallet " + walletId);
                return;
            }
            System.out.println("\nHistory for wallet " + walletId + ":");
            history.forEach(t -> System.out.printf("  #%-4d %-9s %10.2f  %-7s  %s%n",
                    t.getTxnId(), t.getTxnType(), t.getAmount(), t.getStatus(), t.getCreatedAt()));
        } catch (CampusPaymentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void showTopSpenders() {
        System.out.println("\nTop 5 Spenders:");
        reportService.getTopSpenders(5).forEach(s ->
                System.out.printf("  %s %s (%s) - %s across %d txns%n",
                        s.getStudentId(), s.getStudentName(), s.getDepartment(),
                        s.getTotalSpent(), s.getTransactionCount()));
    }

    private void showDepartmentWiseSpend() {
        System.out.println("\nDepartment-wise Spend:");
        reportService.getDepartmentWiseSpend().forEach(d ->
                System.out.printf("  %s - %s (%d txns)%n",
                        d.getDepartment(), d.getTotalSpent(), d.getTransactionCount()));
    }

    private void showMonthlySummary() {
        System.out.println("\nMonthly Summary:");
        reportService.getMonthlySummaries().forEach(m ->
                System.out.printf("  %d-%02d - %s (%d txns)%n",
                        m.getYear(), m.getMonth(), m.getTotalSpent(), m.getTransactionCount()));
    }

    private Long readLong(String label) {
        String raw = prompt(label);
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number: " + raw);
            return null;
        }
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }
}
