package com.campus.service.impl;

import com.campus.constants.ErrorMessages;
import com.campus.entity.Student;
import com.campus.exception.CampusPaymentException;
import com.campus.exception.DuplicateStudentException;
import com.campus.exception.InvalidInputException;
import com.campus.exception.StudentNotFoundException;
import com.campus.repository.interfaces.IStudentRepository;
import com.campus.service.interfaces.IStudentService;
import com.campus.util.Logger;
import com.campus.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for student management (the ONLY place student rules live).
 *
 * WHY  : Keeps validation, duplicate detection, id generation and logging out
 *        of both the repository (data only) and the menu (I/O only) — Single
 *        Responsibility + layered architecture.
 * HOW  : Constructor-injected with IStudentRepository (Dependency Inversion).
 *        Validates with ValidationUtil BEFORE any repository call, throws
 *        specific custom exceptions for business failures, and logs every
 *        outcome through the Logger utility (no System.out here).
 * USED BY : StudentMenu (controller), wired in AppConfig.
 */
public class StudentServiceImpl implements IStudentService {

    /** Prefix + width for generated ids, e.g. STU000001. Fits VARCHAR(20). */
    private static final String ID_PREFIX = "STU";
    private static final int ID_DIGITS = 6;

    private final IStudentRepository studentRepository;

    public StudentServiceImpl(IStudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /** {@inheritDoc} */
    @Override
    public String registerStudent(Student student) throws CampusPaymentException {
        validateProfile(student);                                // 1. field-level validation

        String email = student.getEmail().trim().toLowerCase();  // 2. normalise for uniqueness
        if (studentRepository.findByEmail(email).isPresent()) {  // 3. enforce UNIQUE(email)
            Logger.warning("Registration rejected — email already exists: " + email);
            throw new DuplicateStudentException(ErrorMessages.STUDENT_ALREADY_EXISTS);
        }

        student.setEmail(email);
        student.setStudentId(generateStudentId());               // 4. app-generated PK
        student.setCreatedAt(LocalDateTime.now());
        studentRepository.save(student);                         // 5. persist

        Logger.info("Student registered: " + student.getStudentId() + " (" + email + ")");
        return student.getStudentId();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Student> getStudent(String studentId) {
        if (!ValidationUtil.isValidStudentId(studentId)) {
            return Optional.empty();
        }
        return studentRepository.findById(studentId);
    }

    /** {@inheritDoc} */
    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /** {@inheritDoc} */
    @Override
    public void updateStudent(Student student) throws CampusPaymentException {
        if (student == null || !ValidationUtil.isValidStudentId(student.getStudentId())) {
            throw new InvalidInputException(ErrorMessages.STUDENT_ID_REQUIRED);
        }
        validateProfile(student);

        if (!studentRepository.exists(student.getStudentId())) {
            Logger.warning("Update rejected — no such student: " + student.getStudentId());
            throw new StudentNotFoundException(ErrorMessages.STUDENT_NOT_FOUND);
        }

        student.setEmail(student.getEmail().trim().toLowerCase());
        studentRepository.update(student);
        Logger.info("Student updated: " + student.getStudentId());
    }

    /** {@inheritDoc} */
    @Override
    public void deleteStudent(String studentId) throws CampusPaymentException {
        if (!ValidationUtil.isValidStudentId(studentId)) {
            throw new InvalidInputException(ErrorMessages.STUDENT_ID_REQUIRED);
        }
        if (!studentRepository.exists(studentId)) {
            Logger.warning("Delete rejected — no such student: " + studentId);
            throw new StudentNotFoundException(ErrorMessages.STUDENT_NOT_FOUND);
        }
        studentRepository.delete(studentId);
        Logger.info("Student deleted: " + studentId);
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    /** Validates the NOT NULL profile fields; phone is optional (NULLABLE column). */
    private void validateProfile(Student student) throws InvalidInputException {
        if (student == null) {
            throw new InvalidInputException(ErrorMessages.INVALID_STUDENT_DATA);
        }
        if (!ValidationUtil.isValidName(student.getName())) {
            throw new InvalidInputException(ErrorMessages.INVALID_NAME);
        }
        if (!ValidationUtil.isValidEmail(student.getEmail())) {
            throw new InvalidInputException(ErrorMessages.INVALID_EMAIL);
        }
        if (!ValidationUtil.isValidDepartment(student.getDepartment())) {
            throw new InvalidInputException(ErrorMessages.INVALID_DEPARTMENT);
        }
        if (!ValidationUtil.isValidPhone(student.getPhone())) {
            throw new InvalidInputException(ErrorMessages.INVALID_PHONE);
        }
    }

    /**
     * Derives the next id from the current maximum (e.g. STU000007 -> STU000008).
     * Starts at STU000001 when the table is empty.
     */
    private String generateStudentId() {
        long next = studentRepository.findMaxStudentId()
                .map(this::extractSequence)
                .orElse(0L) + 1;
        return ID_PREFIX + String.format("%0" + ID_DIGITS + "d", next);
    }

    /** Parses the numeric part of an id like "STU000007" -> 7; tolerant of bad data. */
    private long extractSequence(String studentId) {
        try {
            return Long.parseLong(studentId.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
