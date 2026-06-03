package com.campus.config;

import java.math.BigDecimal;

public class AppConfig {
    public static final BigDecimal WALLET_MAX_BALANCE = BigDecimal.valueOf(1000000);
    public static final BigDecimal DAILY_TRANSFER_LIMIT = BigDecimal.valueOf(100000);
    public static final BigDecimal MIN_TRANSFER_AMOUNT = BigDecimal.valueOf(1);
    public static final BigDecimal MAX_TRANSFER_AMOUNT = BigDecimal.valueOf(500000);
    
    public static final int FRAUD_DETECTION_THRESHOLD = 10;
    public static final int FRAUD_TIME_WINDOW_MINUTES = 5;
    
    public static final String APP_NAME = "Campus Payment Platform";
    public static final String APP_VERSION = "1.0.0";
}
