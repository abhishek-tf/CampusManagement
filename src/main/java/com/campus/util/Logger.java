package com.campus.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

/**
 * Single application logger backed by the JDK logging framework ({@link java.util.logging}).
 *
 * <p>WHAT: A thin facade over one shared java.util.logging.Logger that writes to
 *       {@code logs/campuspay.log} at INFO/WARNING/SEVERE, plus audit/paymentFailure/rollback helpers.
 * WHY:  Uses the JDK's own logging (no external library) and a single logger instance so the whole
 *       app logs consistently to one file. A facade keeps call sites terse (Logger.audit(...)) and
 *       means the backend could change without touching callers.
 * HOW:  A FileHandler + SimpleFormatter is attached once; static methods delegate to the logger,
 *       and crucially do NOT call System.out/System.err themselves.</p>
 */
public final class Logger {

    // WHAT: The one shared logger instance, configured at class load.
    // WHY:  static final = a single logger for the whole app (singleton-style access), avoiding
    //       duplicate handlers and giving every class the same destination/format.
    private static final java.util.logging.Logger LOGGER = init();

    private Logger() {
    }

    /**
     * WHAT: Builds and configures the named logger with a file handler.
     * WHY:  Centralised setup; runs exactly once via the static initialiser.
     * HOW:  Creates logs/ (FileHandler cannot create directories), attaches a SimpleFormatter
     *       (human-readable text) in append mode, and guards against adding the handler twice.
     */
    private static java.util.logging.Logger init() {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("campuspay");
        // WHY Level.ALL: let every level through to the handlers; the handlers decide what to write.
        logger.setLevel(Level.ALL);
        // WHY the guard: if the class were initialised more than once we must not stack duplicate
        //     FileHandlers (which would write each line repeatedly).
        if (logger.getHandlers().length == 0) {
            try {
                // WHY: FileHandler will not create missing parent directories, so create logs/ first.
                Files.createDirectories(Paths.get("logs"));
                // WHY append=true: preserve history across runs instead of truncating the log.
                FileHandler fileHandler = new FileHandler("logs/campuspay.log", true);
                // WHY SimpleFormatter: human-readable "timestamp / source / LEVEL: message" text,
                //     suitable for an audit log (vs the default XML formatter).
                fileHandler.setFormatter(new SimpleFormatter());
                fileHandler.setLevel(Level.ALL);
                logger.addHandler(fileHandler);
            } catch (IOException e) {
                // WHY: if the file can't be opened, fall back to the parent console handler rather
                //      than crash — logging must never take down the application.
                logger.log(Level.SEVERE, "Unable to initialise file logging", e);
            }
        }
        return logger;
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warning(String message) {
        LOGGER.warning(message);
    }

    public static void severe(String message) {
        LOGGER.severe(message);
    }

    /** Backwards-compatible alias for {@link #severe(String)} (older call sites used error()). */
    public static void error(String message) {
        LOGGER.severe(message);
    }

    public static void error(String message, Throwable e) {
        // WHY log(Level.SEVERE, msg, throwable): records the stack trace alongside the message.
        LOGGER.log(Level.SEVERE, message, e);
    }

    /**
     * Transaction audit trail (successful money movements).
     * WHY: a tagged INFO entry gives a positive, traceable record of every completed payment.
     */
    public static void audit(String message) {
        LOGGER.info("[AUDIT] " + message);
    }

    /**
     * Payment failure log.
     * WHY: failed payments are logged at SEVERE so they stand out for ops/fraud review and are
     *      easy to grep, separate from routine activity.
     */
    public static void paymentFailure(String message) {
        LOGGER.severe("[PAYMENT-FAILURE] " + message);
    }

    /**
     * Rollback log (compensating action after a failed unit of work).
     * WHY: recording rollbacks documents that the system correctly undid a partial payment,
     *      which is important evidence that atomicity held.
     */
    public static void rollback(String message) {
        LOGGER.warning("[ROLLBACK] " + message);
    }
}