package com.campus.menu;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.campus.entity.ExpenseSplits;
import com.campus.enums.SplitType;
import com.campus.exception.CampusPaymentException;
import com.campus.service.interfaces.IExpenseService;

/**
 * Console controller for Splitwise-style Expense Sharing. All I/O lives here;
 * splitting, settlement and reporting delegate to {@link IExpenseService}.
 */
public class ExpenseMenu {

    private final IExpenseService expenseService;
    private final Scanner scanner;

    public ExpenseMenu(IExpenseService expenseService, Scanner scanner) {
        this.expenseService = expenseService;
        this.scanner = scanner;
    }

    public void show() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Expense Sharing ---");
            System.out.println("1. Create Group");
            System.out.println("2. Add Member");
            System.out.println("3. Add Expense (split a bill)");
            System.out.println("4. View Pending Dues");
            System.out.println("5. Settle a Split");
            System.out.println("6. Group Balances");
            System.out.println("7. Back");
            switch (prompt("Choice")) {
                case "1" -> createGroup();
                case "2" -> addMember();
                case "3" -> addExpense();
                case "4" -> viewPendingDues();
                case "5" -> settleSplit();
                case "6" -> groupBalances();
                case "7" -> back = true;
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private void createGroup() {
        try {
            long groupId = expenseService.createGroup(prompt("Group name"), prompt("Created by (student id)"));
            System.out.println("Group created with id " + groupId);
        } catch (CampusPaymentException e) {
            System.out.println("Could not create group: " + e.getMessage());
        }
    }

    private void addMember() {
        Long groupId = readLong("Group id");
        if (groupId == null) {
            return;
        }
        try {
            expenseService.addMember(groupId, prompt("Student id"));
            System.out.println("Member added.");
        } catch (CampusPaymentException e) {
            System.out.println("Could not add member: " + e.getMessage());
        }
    }

    private void addExpense() {
        Long groupId = readLong("Group id");
        if (groupId == null) {
            return;
        }
        String paidBy = prompt("Paid by (student id)");
        String description = prompt("Description");
        BigDecimal total = readAmount("Total amount");
        if (total == null) {
            return;
        }
        SplitType splitType = readSplitType();
        if (splitType == null) {
            return;
        }
        // EQUAL needs no shares; EXACT/PERCENT need per-participant values.
        Map<String, BigDecimal> shares = splitType == SplitType.EQUAL ? Map.of() : readShares(splitType);
        try {
            long expenseId = expenseService.addExpense(groupId, paidBy, description, total, splitType, shares);
            System.out.println("Expense recorded with id " + expenseId);
        } catch (CampusPaymentException e) {
            System.out.println("Could not add expense: " + e.getMessage());
        }
    }

    private void viewPendingDues() {
        try {
            List<ExpenseSplits> dues = expenseService.getPendingDues(prompt("Student id"));
            if (dues.isEmpty()) {
                System.out.println("No pending dues.");
                return;
            }
            dues.forEach(d -> System.out.printf("  split #%d  expense %d  owes %s%n",
                    d.getSplitId(), d.getExpenseId(), d.getShareAmount()));
        } catch (CampusPaymentException e) {
            System.out.println("Could not load dues: " + e.getMessage());
        }
    }

    private void settleSplit() {
        Long splitId = readLong("Split id to settle");
        if (splitId == null) {
            return;
        }
        try {
            expenseService.settleSplit(splitId);
            System.out.println("Split settled.");
        } catch (CampusPaymentException e) {
            System.out.println("Could not settle: " + e.getMessage());
        }
    }

    private void groupBalances() {
        Long groupId = readLong("Group id");
        if (groupId == null) {
            return;
        }
        try {
            Map<String, BigDecimal> balances = expenseService.getGroupNetBalances(groupId);
            if (balances.isEmpty()) {
                System.out.println("No members / expenses for this group.");
                return;
            }
            balances.forEach((student, net) -> System.out.printf("  %s : %s (%s)%n",
                    student, net, net.signum() >= 0 ? "is owed" : "owes"));
        } catch (CampusPaymentException e) {
            System.out.println("Could not load balances: " + e.getMessage());
        }
    }

    // --- input helpers ----------------------------------------------------

    /** Reads "studentId=value" pairs (one per line, blank to finish). */
    private Map<String, BigDecimal> readShares(SplitType splitType) {
        String unit = splitType == SplitType.PERCENT ? "percent" : "amount";
        System.out.println("Enter participant shares as 'studentId=" + unit + "', blank line to finish:");
        Map<String, BigDecimal> shares = new LinkedHashMap<>();
        while (true) {
            String line = prompt("  share");
            if (line.isBlank()) {
                break;
            }
            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                System.out.println("  expected studentId=" + unit);
                continue;
            }
            try {
                shares.put(parts[0].trim(), new BigDecimal(parts[1].trim()));
            } catch (NumberFormatException e) {
                System.out.println("  invalid number: " + parts[1]);
            }
        }
        return shares;
    }

    private SplitType readSplitType() {
        String raw = prompt("Split type (EQUAL/EXACT/PERCENT)");
        try {
            return SplitType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid split type: " + raw);
            return null;
        }
    }

    private BigDecimal readAmount(String label) {
        String raw = prompt(label);
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount: " + raw);
            return null;
        }
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
