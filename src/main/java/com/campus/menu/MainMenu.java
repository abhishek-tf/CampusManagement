package com.campus.menu;

import java.util.Scanner;

import com.campus.config.AppConfig;
import com.campus.util.Logger;

/**
 * Top-level console controller. Dispatches to one focused sub-menu per module
 * (Single Responsibility) and obtains every service from {@link AppConfig} (the
 * composition root), so this class wires nothing itself.
 */
public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);

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
                    case "5" -> new ReportMenu(AppConfig.getReportService(),
                            AppConfig.getTransactionService(), scanner).show();
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

    /** Reads one line; returns null when the input stream has no more lines (EOF). */
    private String prompt(String label) {
        System.out.print(label);
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }
}
