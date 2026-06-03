package com.campus.util;

public class Logger {
    public static void info(String message) {
        System.out.println("[INFO] " + getCurrentTimestamp() + " - " + message);
    }

    public static void error(String message) {
        System.err.println("[ERROR] " + getCurrentTimestamp() + " - " + message);
    }

    public static void error(String message, Exception e) {
        System.err.println("[ERROR] " + getCurrentTimestamp() + " - " + message);
        e.printStackTrace();
    }

    public static void warning(String message) {
        System.out.println("[WARNING] " + getCurrentTimestamp() + " - " + message);
    }

    private static String getCurrentTimestamp() {
        return java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
    }
}
