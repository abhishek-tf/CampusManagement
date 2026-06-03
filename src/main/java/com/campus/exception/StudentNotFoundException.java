package com.campus.exception;

public class StudentNotFoundException extends CampusPaymentException {
    public StudentNotFoundException(String message) {
        super(message, "STUDENT_NOT_FOUND");
    }
}
