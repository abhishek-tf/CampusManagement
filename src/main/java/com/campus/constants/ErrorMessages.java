package com.campus.constants;

public class ErrorMessages {
    public static final String STUDENT_NOT_FOUND = "Student not found";
    // --- Student module messages (kept here so no message is hardcoded in services) ---
    public static final String STUDENT_ALREADY_EXISTS = "A student with this email already exists";
    public static final String INVALID_STUDENT_DATA = "Student data is missing or invalid";
    public static final String INVALID_NAME = "Name is required";
    public static final String INVALID_EMAIL = "A valid email is required";
    public static final String INVALID_DEPARTMENT = "Department is required";
    public static final String INVALID_PHONE = "Phone must be 10 digits";
    public static final String STUDENT_ID_REQUIRED = "Student id is required";
    public static final String INSUFFICIENT_BALANCE = "Insufficient balance";
    public static final String INVALID_AMOUNT = "Invalid amount";
    public static final String DAILY_LIMIT_EXCEEDED = "Daily limit exceeded";
    public static final String FRAUD_DETECTED = "Fraudulent activity detected";
    public static final String INVALID_INPUT = "Invalid input";
    public static final String DATABASE_ERROR = "Database error occurred";
    public static final String OPERATION_FAILED = "Operation failed";

    // Expense sharing
    public static final String GROUP_NOT_FOUND = "Expense group not found";
    public static final String EXPENSE_NOT_FOUND = "Expense not found";
    public static final String SPLIT_NOT_FOUND = "Expense split not found";
    public static final String NOT_GROUP_MEMBER = "Student is not a member of this group";
    public static final String ALREADY_SETTLED = "Expense split is already settled";
    public static final String INVALID_SPLIT = "Split amounts do not add up to the total";
}
