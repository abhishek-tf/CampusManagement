package com.campus.menu;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.campus.entity.CampusPayment;
import com.campus.enums.PaymentCategory;
import com.campus.exception.CampusPaymentException;
import com.campus.service.interfaces.IPaymentService;

/**
 * Console controller for Campus Payments. All I/O lives here; payment execution
 * delegates to {@link IPaymentService}.
 */
public class PaymentMenu {

    private final IPaymentService paymentService;
    private final Scanner scanner;

    public PaymentMenu(IPaymentService paymentService, Scanner scanner) {
        this.paymentService = paymentService;
        this.scanner = scanner;
    }

    public void show() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Payment Management ---");
            System.out.println("1. Make Payment");
            System.out.println("2. View Payment History");
            System.out.println("3. Back");
            switch (prompt("Choice")) {
                case "1" -> makePayment();
                case "2" -> viewPayments();
                case "3" -> back = true;
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private void makePayment() {
        String studentId = prompt("Student id");
        System.out.println("Categories: " + Arrays.toString(PaymentCategory.values()));
        String category = prompt("Category");
        String rawAmount = prompt("Amount");
        try {
            paymentService.processPayment(studentId, category, new BigDecimal(rawAmount));
            System.out.println("Payment successful.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount: " + rawAmount);
        } catch (CampusPaymentException e) {
            System.out.println("Payment failed: " + e.getMessage());
        }
    }

    private void viewPayments() {
        try {
            List<CampusPayment> payments = paymentService.getPaymentHistory(prompt("Student id"));
            if (payments.isEmpty()) {
                System.out.println("No payments found.");
                return;
            }
            payments.forEach(p -> System.out.printf("  #%d  %-13s  %s  (txn %d)  %s%n",
                    p.getPaymentId(), p.getCategory(), p.getAmount(), p.getTxnId(), p.getPaidAt()));
        } catch (CampusPaymentException e) {
            System.out.println("Could not load payments: " + e.getMessage());
        }
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }
}
