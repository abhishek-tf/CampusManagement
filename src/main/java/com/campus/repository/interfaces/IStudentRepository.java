package com.campus.repository.interfaces;

import com.campus.entity.Student;
import java.util.Optional;
import java.util.List;

/**
 * Persistence contract for the `student` table.
 *
 * WHY  : Depending on this interface (not the impl) keeps the service layer
 *        decoupled from JDBC (Dependency Inversion). Only data operations live
 *        here — no validation, no id generation, no business rules.
 * HOW  : CRUD plus two read helpers the service needs: findByEmail (duplicate
 *        check) and findMaxStudentId (id generation).
 * USED BY : StudentServiceImpl (via constructor injection).
 *
 * Note: studentId is String because student_id is VARCHAR(20) in schema.sql.
 */
public interface IStudentRepository {
    void save(Student student);
    Optional<Student> findById(String studentId);
    Optional<Student> findByEmail(String email);
    List<Student> findAll();
    void update(Student student);
    void delete(String studentId);
    boolean exists(String studentId);

    /**
     * @return the highest existing student_id (e.g. "STU000007"), or empty when
     *         the table has no rows. Used by the service to derive the next id.
     */
    Optional<String> findMaxStudentId();
}
