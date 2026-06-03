package com.campus.service.interfaces;

import com.campus.entity.Student;
import com.campus.exception.CampusPaymentException;
import java.util.List;
import java.util.Optional;

/**
 * Business contract for student management.
 *
 * WHY  : A small, focused interface (Interface Segregation) so menu/controllers
 *        depend on the abstraction, not StudentServiceImpl (Dependency Inversion).
 * HOW  : Declares only student use-cases; checked CampusPaymentException signals
 *        a recoverable business failure (validation / duplicate / not-found).
 * USED BY : StudentMenu (controller layer), AppConfig (wiring).
 */
public interface IStudentService {

    /**
     * Validates and persists a new student, generating its student_id.
     * @return the generated student_id (e.g. "STU000001").
     * @throws CampusPaymentException if data is invalid or the email already exists.
     */
    String registerStudent(Student student) throws CampusPaymentException;

    /** @return the student, or empty if no row has that id. */
    Optional<Student> getStudent(String studentId);

    List<Student> getAllStudents();

    /** @throws CampusPaymentException if data is invalid or the student does not exist. */
    void updateStudent(Student student) throws CampusPaymentException;

    /** @throws CampusPaymentException if the student does not exist. */
    void deleteStudent(String studentId) throws CampusPaymentException;
}
