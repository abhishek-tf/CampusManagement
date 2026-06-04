package com.campus.config;

import java.math.BigDecimal;

import com.campus.repository.impl.ExpenseRepositoryImpl;
import com.campus.repository.impl.PaymentRepositoryImpl;
import com.campus.repository.impl.ReportRepositoryImpl;
import com.campus.repository.impl.SplitRepositoryImpl;
import com.campus.repository.impl.StudentRepositoryImpl;
import com.campus.repository.impl.TransactionRepositoryImpl;
import com.campus.repository.impl.WalletRepositoryImpl;
import com.campus.repository.interfaces.IPaymentRepository;
import com.campus.repository.interfaces.IWalletRepository;
import com.campus.service.impl.ExpenseServiceImpl;
import com.campus.service.impl.PaymentServiceImpl;
import com.campus.service.impl.ReportServiceImpl;
import com.campus.service.impl.StudentServiceImpl;
import com.campus.service.impl.TransactionServiceImpl;
import com.campus.service.impl.WalletServiceImpl;
import com.campus.service.interfaces.IExpenseService;
import com.campus.service.interfaces.IPaymentService;
import com.campus.service.interfaces.IReportService;
import com.campus.service.interfaces.IStudentService;
import com.campus.service.interfaces.ITransactionService;
import com.campus.service.interfaces.IWalletService;

/**
 * Composition root: app-wide constants plus the single place that wires
 * repositories into services via constructor injection (Dependency Inversion).
 *
 * <p>All repositories obtain JDBC connections from the shared
 * {@link com.campus.util.DBConnection} factory (which reads db.properties), so
 * there is one connection strategy across the whole app. Callers depend only on
 * the service interfaces returned here, never on the implementations.</p>
 */
public final class AppConfig {

    // --- App constants ---
    public static final BigDecimal WALLET_MAX_BALANCE = BigDecimal.valueOf(1000000);
    public static final BigDecimal DAILY_TRANSFER_LIMIT = BigDecimal.valueOf(100000);
    public static final BigDecimal MIN_TRANSFER_AMOUNT = BigDecimal.valueOf(1);
    public static final BigDecimal MAX_TRANSFER_AMOUNT = BigDecimal.valueOf(500000);

    public static final String APP_NAME = "Campus Payment Platform";
    public static final String APP_VERSION = "1.0.0";

    private AppConfig() {
    }

    public static IStudentService getStudentService() {
        return new StudentServiceImpl(new StudentRepositoryImpl());
    }

    public static IWalletService getWalletService() {
        return new WalletServiceImpl(new WalletRepositoryImpl());
    }

    public static IPaymentService getPaymentService() {
        IPaymentRepository paymentRepository = new PaymentRepositoryImpl();
        IWalletRepository walletRepository = new WalletRepositoryImpl();
        return new PaymentServiceImpl(paymentRepository, walletRepository);
    }

    public static IExpenseService getExpenseService() {
        return new ExpenseServiceImpl(new ExpenseRepositoryImpl(), new SplitRepositoryImpl());
    }

    public static IReportService getReportService() {
        return new ReportServiceImpl(new ReportRepositoryImpl());
    }

    public static ITransactionService getTransactionService() {
        return new TransactionServiceImpl(new TransactionRepositoryImpl());
    }
}
