package com.campus.config;

import com.campus.repository.impl.StudentRepositoryImpl;
import com.campus.repository.interfaces.IStudentRepository;
import com.campus.service.impl.StudentServiceImpl;
import com.campus.service.interfaces.IStudentService;

import javax.sql.DataSource;
import java.math.BigDecimal;

/**
 * Application configuration: app-wide constants + dependency wiring.
 *
 * WHY  : One composition root that builds repositories and services with
 *        constructor injection, so no class news-up its own dependencies.
 * HOW  : Lazily builds singletons from a DataSource. The wiring methods select
 *        the interface types (IStudentRepository/IStudentService) so callers
 *        stay decoupled from the implementations (Dependency Inversion).
 * USED BY : MainMenu / StudentMenu to obtain a ready IStudentService.
 *
 * DB CONNECTION SEAM:
 *   This class does NOT create database connections — that is owned by the
 *   DB-connection module (teammate). That module is expected to build the
 *   DataSource (from db.properties) and hand it in once at startup via
 *   {@link #configureDataSource(DataSource)}. Nothing here touches
 *   DriverManager or db.properties.
 */
public class AppConfig {

    // --- App constants ---
    public static final BigDecimal WALLET_MAX_BALANCE = BigDecimal.valueOf(1000000);
    public static final BigDecimal DAILY_TRANSFER_LIMIT = BigDecimal.valueOf(100000);
    public static final BigDecimal MIN_TRANSFER_AMOUNT = BigDecimal.valueOf(1);
    public static final BigDecimal MAX_TRANSFER_AMOUNT = BigDecimal.valueOf(500000);

    public static final int FRAUD_DETECTION_THRESHOLD = 10;
    public static final int FRAUD_TIME_WINDOW_MINUTES = 5;

    public static final String APP_NAME = "Campus Payment Platform";
    public static final String APP_VERSION = "1.0.0";

    // --- Wiring state ---
    private static DataSource dataSource;          // injected by the DB-connection module
    private static IStudentService studentService; // lazily built singleton

    private AppConfig() { }                        // no instances — static composition root

    /**
     * Entry point for the DB-connection module to supply the configured DataSource.
     * Call once at startup, before any service is requested.
     */
    public static void configureDataSource(DataSource configuredDataSource) {
        dataSource = configuredDataSource;
    }

    /**
     * @return the wired student service (built once, reused thereafter).
     * @throws IllegalStateException if the DataSource has not been configured yet
     *         — a setup error, signalling the DB-connection module ran too late.
     */
    public static IStudentService getStudentService() {
        if (studentService == null) {
            if (dataSource == null) {
                throw new IllegalStateException(
                        "DataSource not configured — the DB-connection module must call "
                                + "AppConfig.configureDataSource(...) at startup.");
            }
            IStudentRepository studentRepository = new StudentRepositoryImpl(dataSource);
            studentService = new StudentServiceImpl(studentRepository);
        }
        return studentService;
    }
}
