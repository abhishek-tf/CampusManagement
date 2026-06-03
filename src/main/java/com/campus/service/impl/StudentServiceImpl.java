package com.campus.service.impl;

import com.campus.entity.Student;
import com.campus.exception.CampusPaymentException;
import com.campus.exception.StudentNotFoundException;
import com.campus.repository.interfaces.IStudentRepository;
import com.campus.service.interfaces.IStudentService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class StudentServiceImpl implements IStudentService {
    private final IStudentRepository studentRepository;

    public StudentServiceImpl(IStudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public void registerStudent(Student student) throws CampusPaymentException {
        if (student == null || student.getEmail() == null) {
            throw new CampusPaymentException("Invalid student data");
        }
        
        if (studentRepository.findByEmail(student.getEmail()).isPresent()) {
            throw new CampusPaymentException("Student already exists");
        }

        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        studentRepository.save(student);
    }

    @Override
    public Optional<Student> getStudent(Long studentId) {
        return studentRepository.findById(studentId);
    }

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public void updateStudent(Student student) throws CampusPaymentException {
        if (student == null || student.getStudentId() == null) {
            throw new CampusPaymentException("Invalid student data");
        }

        if (!studentRepository.exists(student.getStudentId())) {
            throw new StudentNotFoundException("Student not found");
        }

        student.setUpdatedAt(LocalDateTime.now());
        studentRepository.update(student);
    }
}
