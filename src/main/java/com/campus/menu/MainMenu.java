package com.campus.menu;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

import com.campus.config.AppConfig;
import com.campus.entity.Transaction;
import com.campus.exception.CampusPaymentException;
import com.campus.service.interfaces.IReportService;
import com.campus.service.interfaces.ITransactionService;

public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final IReportService reportService;
    private final ITransactionService transactionService;

    public MainMenu(IReportService reportService, ITransactionService transactionService) {
        this.reportService = reportService;
        this.transactionService = transactionService;
    }

    public void start() {
        boolean running = true;

        while (running) {
            displayMenu();
            String choice = prompt("Enter choice: ");
            if (choice == null) {
                break; // input stream ended
            }

            switch (choice) {
                case "1" -> handleStudentMenu();
                case "2" -> handleWalletMenu();
                case "3" -> handlePaymentMenu();
                case "4" -> handleExpenseMenu();
                case "5" -> handleReportsMenu();
                case "6" -> {
                    System.out.println("Exiting...");
                    running = false;
                }
                default -> System.out.println("Invalid choice");
            }
        }
        scanner.close();
    }

    private void displayMenu() {
        System.out.println("\n--- Campus Payment Platform ---");
        System.out.println("1. Student Management");
        System.out.println("2. Wallet Management");
        System.out.println("3. Payment Management");
        System.out.println("4. Expense Management");
        System.out.println("5. Transaction History & Reports");
        System.out.println("6. Exit");
    }

    private void handleStudentMenu() {
        try {
            new StudentMenu(AppConfig.getStudentService(), scanner).show();
        } catch (IllegalStateException e) {
            System.out.println("Student module unavailable: " + e.getMessage());
        }
    }

    private void handleWalletMenu() {
        System.out.println("\n--- Wallet Management ---");
        System.out.println("1. Top Up");
        System.out.println("2. Withdraw");
        System.out.println("3. Transfer");
        System.out.print("Choice: ");
    }

    private void handlePaymentMenu() {
        System.out.println("\n--- Payment Management ---");
        System.out.println("1. Process Payment");
        System.out.println("2. View Payments");
        System.out.print("Choice: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> processPayment();
            case "2" -> viewPayments();
            default -> System.out.println("Invalid choice");
        }
    }

    private void processPayment() {
        try {
            System.out.print("Student ID: ");
            String studentId = scanner.nextLine().trim();

            System.out.println(
                    "Categories: "
                            + java.util.Arrays.toString(
                                    PaymentCategory.values()));

            System.out.print("Category: ");
            String category = scanner.nextLine().trim();

            System.out.print("Amount: ");
            BigDecimal amount =
                    new BigDecimal(scanner.nextLine().trim());

            paymentService.processPayment(
                    studentId,
                    category,
                    amount);

            System.out.println("Payment successful.");

        } catch (NumberFormatException e) {

            System.out.println("Invalid amount.");

        } catch (CampusPaymentException e) {

            System.out.println(
                    "Payment failed: " + e.getMessage());

            Logger.warning(
                    "Payment menu reported failure: "
                            + e.getMessage());
        }
    }

    private void viewPayments() {
        try {
            System.out.print("Student ID: ");
            String studentId = scanner.nextLine().trim();

            List<CampusPayment> payments =
                    paymentService.getPaymentHistory(studentId);

            if (payments.isEmpty()) {
                System.out.println("No payments found.");
                return;
            }

            payments.forEach(
                    p -> System.out.printf(
                            "#%d  %-13s  %s  (txn %d)  %s%n",
                            p.getPaymentId(),
                            p.getCategory(),
                            p.getAmount(),
                            p.getTxnId(),
                            p.getPaidAt()));

        } catch (CampusPaymentException e) {
            System.out.println(
                    "Could not load payments: "
                            + e.getMessage());
        }
    }

    private void handleExpenseMenu() {
        System.out.println("\n--- Expense Management ---");
        System.out.println("1. Create Group");
        System.out.println("2. Split Expense");
        System.out.print("Choice: ");
    }

    private void handleReportsMenu() {
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
                case "3" -> showTotalSpend();
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
            BigDecimal amount = new BigDecimal(rawAmount.trim());
            Transaction txn = transactionService.recordTransaction(walletId, type, amount);
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

    private void showTotalSpend() {
        System.out.println("\nTotal Spend: " + reportService.getTotalSpend());
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

    private void 
    showMonthlySummary() {
        System.out.println("\nMonthly Summary:");
        reportService.getMonthlySummaries().forEach(m ->
                System.out.printf("  %d-%02d - %s (%d txns)%n",
                        m.getYear(), m.getMonth(), m.getTotalSpent(), m.getTransactionCount()));
    }

    /** Prompts for a wallet id; returns null if input is invalid or the stream ends. */
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
