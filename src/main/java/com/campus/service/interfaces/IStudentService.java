package com.campus.service.interfaces;

import com.campus.entity.Student;
import com.campus.exception.CampusPaymentException;
import java.util.List;
import java.util.Optional;

public interface IStudentService {
    void registerStudent(Student student) throws CampusPaymentException;
    Optional<Student> getStudent(Long studentId);
    List<Student> getAllStudents();
    void updateStudent(Student student) throws CampusPaymentException;
}
