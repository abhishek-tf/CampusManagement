package com.campus.menu;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

import com.campus.config.AppConfig;
import com.campus.entity.Transaction;
import com.campus.exception.CampusPaymentException;
import com.campus.service.interfaces.IReportService;
import com.campus.service.interfaces.ITransactionService;
import com.campus.util.Logger;

/**
 * Top-level console controller. Dispatches to one focused sub-menu per module
 * (Single Responsibility) and obtains every service from {@link AppConfig} (the
 * composition root), so this class wires nothing itself.
 */
public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final IReportService reportService = AppConfig.getReportService();
    private final ITransactionService transactionService = AppConfig.getTransactionService();

    public void start() {
        boolean running = true;
        while (running) {
            displayMenu();
            String choice = prompt("Enter choice: ");
            if (choice == null) {
                break; // input stream ended
            }
            try {
                switch (choice) {
                    case "1" -> new StudentMenu(AppConfig.getStudentService(), scanner).show();
                    case "2" -> new WalletMenu(AppConfig.getWalletService(), scanner).show();
                    case "3" -> new PaymentMenu(AppConfig.getPaymentService(), scanner).show();
                    case "4" -> new ExpenseMenu(AppConfig.getExpenseService(), scanner).show();
                    case "5" -> reportsMenu();
                    case "6" -> {
                        System.out.println("Exiting...");
                        running = false;
                    }
                    default -> System.out.println("Invalid choice");
                }
            } catch (RuntimeException e) {
                // Safety net: an unexpected failure (e.g. database unreachable) must
                // not crash the app — log it and return to the menu.
                Logger.error("Menu operation failed", e);
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
        scanner.close();
    }

    private void displayMenu() {
        System.out.println("\n--- Campus Payment Platform ---");
        System.out.println("1. Student Management");
        System.out.println("2. Wallet Management");
        System.out.println("3. Payment Management");
        System.out.println("4. Expense Sharing");
        System.out.println("5. Transaction History & Reports");
        System.out.println("6. Exit");
    }

    // --- Transaction history & reports (Java Streams) ---------------------

    private void reportsMenu() {
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
            String choice = prompt("Choice: ");
            if (choice == null) {
                return;
            }
            switch (choice) {
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
        Long walletId = readWalletId();
        if (walletId == null) {
            return;
        }
        String type = prompt("Type (DEPOSIT/WITHDRAW/TRANSFER/PAYMENT): ");
        String rawAmount = prompt("Amount: ");
        if (type == null || rawAmount == null) {
            return;
        }
        try {
            Transaction txn = transactionService.recordTransaction(walletId, type, new BigDecimal(rawAmount.trim()));
            System.out.println("Recorded transaction #" + txn.getTxnId()
                    + " (" + txn.getTxnType() + " " + txn.getAmount() + ")");
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount: " + rawAmount);
        } catch (CampusPaymentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewWalletHistory() {
        Long walletId = readWalletId();
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

    // --- shared input helpers ---------------------------------------------

    private Long readWalletId() {
        String raw = prompt("Enter wallet id: ");
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid wallet id: " + raw);
            return null;
        }
    }

    /** Reads one line; returns null when the input stream has no more lines (EOF). */
    private String prompt(String label) {
        System.out.print(label);
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }
}
