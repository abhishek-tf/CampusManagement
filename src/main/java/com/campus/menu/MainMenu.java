package com.campus.menu;

import com.campus.config.AppConfig;
import com.campus.entity.CampusPayment;
import com.campus.enums.PaymentCategory;
import com.campus.exception.CampusPaymentException;
import com.campus.service.interfaces.IPaymentService;
import com.campus.util.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);

    // Campus Payments service
    private final IPaymentService paymentService = AppConfig.buildPaymentService();

    public void start() {
        boolean running = true;

        while (running) {
            displayMenu();
            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleStudentMenu();
                case "2" -> handleWalletMenu();
                case "3" -> handlePaymentMenu();
                case "4" -> handleExpenseMenu();
                case "5" -> {
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
        System.out.println("5. Exit");
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
}