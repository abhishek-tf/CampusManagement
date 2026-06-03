package com.campus;

import com.campus.menu.MainMenu;
import com.campus.repository.impl.ReportRepositoryImpl;
import com.campus.repository.impl.TransactionRepositoryImpl;
import com.campus.repository.interfaces.IReportRepository;
import com.campus.repository.interfaces.ITransactionRepository;
import com.campus.service.impl.ReportServiceImpl;
import com.campus.service.impl.TransactionServiceImpl;
import com.campus.service.interfaces.IReportService;
import com.campus.service.interfaces.ITransactionService;

public class Main {
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║    Campus Payment Platform v1.0.0             ║");
        System.out.println("║    Secure Payment & Expense Management         ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        // Repository layer (JDBC -> MySQL)
        ITransactionRepository transactionRepository = new TransactionRepositoryImpl();
        IReportRepository reportRepository = new ReportRepositoryImpl();

        // Service layer
        IReportService reportService = new ReportServiceImpl(reportRepository);
        ITransactionService transactionService = new TransactionServiceImpl(transactionRepository);

        MainMenu mainMenu = new MainMenu(reportService, transactionService);
        mainMenu.start();
    }
}
