package com.campus.menu;

import java.util.Scanner;

public class MainMenu {
    private final Scanner scanner = new Scanner(System.in);

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
        System.out.println("\n--- Student Management ---");
        System.out.println("1. Register Student");
        System.out.println("2. View Student");
        System.out.print("Choice: ");
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
    }

    private void handleExpenseMenu() {
        System.out.println("\n--- Expense Management ---");
        System.out.println("1. Create Group");
        System.out.println("2. Split Expense");
        System.out.print("Choice: ");
    }
}
