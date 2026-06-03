package com.campus.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateTimeUtil {
    public static boolean isWithinTimeWindow(LocalDateTime timestamp, int windowMinutes) {
        if (timestamp == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        long minutesDifference = ChronoUnit.MINUTES.between(timestamp, now);
        return minutesDifference <= windowMinutes && minutesDifference >= 0;
    }

    public static boolean isNewDay(LocalDateTime lastReset) {
        if (lastReset == null) {
            return true;
        }
        return !lastReset.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }
}
