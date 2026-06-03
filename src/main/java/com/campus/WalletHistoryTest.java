package com.campus;

import com.campus.entity.Transaction;
import com.campus.exception.CampusPaymentException;
import com.campus.repository.impl.TransactionRepositoryImpl;
import com.campus.repository.interfaces.ITransactionRepository;
import com.campus.service.impl.TransactionServiceImpl;
import com.campus.service.interfaces.ITransactionService;

import java.util.List;
import java.util.Scanner;

/**
 * Throwaway verifier: asks the user for a wallet id and prints that wallet's
 * transaction history from the live database.
 */
public class WalletHistoryTest {
    public static void main(String[] args) {
        ITransactionRepository transactionRepository = new TransactionRepositoryImpl();
        ITransactionService transactionService = new TransactionServiceImpl(transactionRepository);

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter wallet id: ");
        Long walletId = parseWalletId(scanner.nextLine());
        if (walletId == null) {
            return;
        }

        try {
            List<Transaction> history = transactionService.getWalletHistory(walletId);
            if (history.isEmpty()) {
                System.out.println("No transactions found for wallet " + walletId);
                return;
            }
            System.out.println("\nTransaction history for wallet " + walletId + ":");
            history.forEach(t -> System.out.printf("  #%-4d %-9s %10.2f  %-7s  %s%n",
                    t.getTxnId(), t.getTxnType(), t.getAmount(), t.getStatus(), t.getCreatedAt()));
        } catch (CampusPaymentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static Long parseWalletId(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid wallet id: " + raw);
            return null;
        }
    }
}
