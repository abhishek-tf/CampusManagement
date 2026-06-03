package com.campus.repository.interfaces;

import com.campus.entity.Student;
import java.util.Optional;
import java.util.List;

public interface IStudentRepository {
    void save(Student student);
    Optional<Student> findById(Long studentId);
    Optional<Student> findByEmail(String email);
    List<Student> findAll();
    void update(Student student);
    void delete(Long studentId);
    boolean exists(Long studentId);
}
