package com.campus.repository.impl;

import com.campus.entity.Student;
import com.campus.repository.interfaces.IStudentRepository;
import java.util.*;

public class StudentRepositoryImpl implements IStudentRepository {
    private final Map<Long, Student> store = new HashMap<>();
    private static long idCounter = 1;

    @Override
    public void save(Student student) {
        if (student.getStudentId() == null) {
            student.setStudentId(idCounter++);
        }
        store.put(student.getStudentId(), student);
    }

    @Override
    public Optional<Student> findById(Long studentId) {
        return Optional.ofNullable(store.get(studentId));
    }

    @Override
    public Optional<Student> findByEmail(String email) {
        return store.values().stream()
                .filter(s -> s.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public List<Student> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void update(Student student) {
        if (store.containsKey(student.getStudentId())) {
            store.put(student.getStudentId(), student);
        }
    }

    @Override
    public void delete(Long studentId) {
        store.remove(studentId);
    }

    @Override
    public boolean exists(Long studentId) {
        return store.containsKey(studentId);
    }
}
