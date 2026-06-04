package com.campus.menu;

import java.math.BigDecimal;
import java.util.Scanner;

import com.campus.dto.WalletDTO;
import com.campus.exception.CampusPaymentException;
import com.campus.service.interfaces.IWalletService;

/**
 * Console controller for the Digital Wallet module. All I/O lives here; every
 * operation delegates to {@link IWalletService} (layered architecture).
 */
public class WalletMenu {

    private final IWalletService walletService;
    private final Scanner scanner;

    public WalletMenu(IWalletService walletService, Scanner scanner) {
        this.walletService = walletService;
        this.scanner = scanner;
    }

    public void show() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Wallet Management ---");
            System.out.println("1. Create Wallet");
            System.out.println("2. Top Up");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. View Balance");
            System.out.println("6. Back");
            switch (prompt("Choice")) {
                case "1" -> createWallet();
                case "2" -> topUp();
                case "3" -> withdraw();
                case "4" -> transfer();
                case "5" -> viewBalance();
                case "6" -> back = true;
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private void createWallet() {
        try {
            WalletDTO wallet = walletService.createWallet(prompt("Student id"));
            System.out.println("Wallet created: id=" + wallet.getWalletId() + " balance=" + wallet.getBalance());
        } catch (CampusPaymentException e) {
            System.out.println("Could not create wallet: " + e.getMessage());
        }
    }

    private void topUp() {
        String studentId = prompt("Student id");
        BigDecimal amount = readAmount();
        if (amount == null) {
            return;
        }
        try {
            walletService.topupWallet(studentId, amount);
            System.out.println("Top-up successful. New balance: " + walletService.getBalance(studentId));
        } catch (CampusPaymentException e) {
            System.out.println("Top-up failed: " + e.getMessage());
        }
    }

    private void withdraw() {
        String studentId = prompt("Student id");
        BigDecimal amount = readAmount();
        if (amount == null) {
            return;
        }
        try {
            walletService.withdrawFromWallet(studentId, amount);
            System.out.println("Withdrawal successful. New balance: " + walletService.getBalance(studentId));
        } catch (CampusPaymentException e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
        }
    }

    private void transfer() {
        String from = prompt("From student id");
        String to = prompt("To student id");
        BigDecimal amount = readAmount();
        if (amount == null) {
            return;
        }
        try {
            walletService.transferMoney(from, to, amount);
            System.out.println("Transfer successful.");
        } catch (CampusPaymentException e) {
            System.out.println("Transfer failed: " + e.getMessage());
        }
    }

    private void viewBalance() {
        try {
            WalletDTO wallet = walletService.getWalletDetails(prompt("Student id"));
            System.out.printf("Balance: %s | Daily used: %s / %s | Cap: %s%n",
                    wallet.getBalance(), wallet.getDailyTransferUsed(),
                    wallet.getDailyTransferLimit(), wallet.getMaxBalanceCap());
        } catch (CampusPaymentException e) {
            System.out.println("Could not load wallet: " + e.getMessage());
        }
    }

    private BigDecimal readAmount() {
        String raw = prompt("Amount");
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount: " + raw);
            return null;
        }
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }
}
