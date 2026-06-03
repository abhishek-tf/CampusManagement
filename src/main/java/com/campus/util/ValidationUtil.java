package com.campus.util;

import java.math.BigDecimal;

public class ValidationUtil {
    public static boolean isValidStudentId(Long studentId) {
        return studentId != null && studentId > 0;
    }

    public static boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^[0-9]{10}$");
    }
}
