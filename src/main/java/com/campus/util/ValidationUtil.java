package com.campus.util;

import java.math.BigDecimal;

/**
 * Stateless input-validation helpers.
 *
 * WHY  : Centralises field-level checks so services validate BEFORE touching
 *        the repository (CLAUDE.md: validation happens before repository calls)
 *        and no two classes re-implement the same rule (DRY).
 * HOW  : Pure static boolean predicates — each rule mirrors a schema constraint
 *        (e.g. student_id is VARCHAR(20), email is UNIQUE/NOT NULL).
 * USED BY : StudentServiceImpl (and other services) before persistence.
 */
public class ValidationUtil {

    /** student_id is VARCHAR(20) and the PK — must be non-blank and within length. */
    public static boolean isValidStudentId(String studentId) {
        return studentId != null && !studentId.isBlank() && studentId.length() <= 20;
    }

    /** name is VARCHAR(100) NOT NULL. */
    public static boolean isValidName(String name) {
        return name != null && !name.isBlank() && name.length() <= 100;
    }

    /** department is VARCHAR(60) NOT NULL. */
    public static boolean isValidDepartment(String department) {
        return department != null && !department.isBlank() && department.length() <= 60;
    }

    public static boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /** email is NOT NULL UNIQUE; basic shape check, capped at VARCHAR(150). */
    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= 150
                && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /** phone is NULLABLE: null is acceptable, but if present it must be 10 digits. */
    public static boolean isValidPhone(String phone) {
        return phone == null || phone.isBlank() || phone.matches("^[0-9]{10}$");
    }
}
