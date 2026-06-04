package com.campus.repository.impl;

import com.campus.constants.ErrorMessages;
import com.campus.entity.Student;
import com.campus.exception.DataAccessException;
import com.campus.repository.interfaces.IStudentRepository;
import com.campus.util.DBConnection;
import com.campus.util.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of IStudentRepository against the MySQL `student` table.
 *
 * WHY  : CLAUDE.md mandates Database-First / MySQL persistence; this replaces
 *        the old in-memory HashMap so student data actually survives.
 * HOW  : Plain JDBC with PreparedStatements (guards against SQL injection) and
 *        try-with-resources (guarantees Connection/Statement/ResultSet close).
 *        Every method opens a Connection from the shared DBConnection factory for
 *        the duration of one operation and closes it on exit.
 *        SQLExceptions are logged and rethrown as an unchecked DataAccessException
 *        so the data-access concern never leaks into business signatures.
 *        This class does NO validation and NO id generation — those are the
 *        service's job (Single Responsibility).
 * USED BY : StudentServiceImpl (through the IStudentRepository interface).
 *
 * DB CONNECTION: connections come from the shared DBConnection factory (which
 *        reads db.properties); this class never touches DriverManager directly.
 */
public class StudentRepositoryImpl implements IStudentRepository {

    public StudentRepositoryImpl() {
        // Connections are obtained from the shared DBConnection factory.
    }

    @Override
    public void save(Student student) {
        // student_id is the app-supplied PK (not AUTO_INCREMENT) so we insert all columns.
        final String sql = "INSERT INTO student "
                + "(student_id, name, email, department, phone, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, student.getStudentId());
            ps.setString(2, student.getName());
            ps.setString(3, student.getEmail());
            ps.setString(4, student.getDepartment());
            ps.setString(5, student.getPhone());          // null is allowed (column is NULLABLE)
            ps.setTimestamp(6, Timestamp.valueOf(student.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw handle("save student " + student.getStudentId(), e);
        }
    }

    @Override
    public Optional<Student> findById(String studentId) {
        final String sql = "SELECT * FROM student WHERE student_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw handle("findById " + studentId, e);
        }
    }

    @Override
    public Optional<Student> findByEmail(String email) {
        final String sql = "SELECT * FROM student WHERE email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw handle("findByEmail " + email, e);
        }
    }

    @Override
    public List<Student> findAll() {
        final String sql = "SELECT * FROM student ORDER BY student_id";
        List<Student> students = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                students.add(mapRow(rs));
            }
            return students;
        } catch (SQLException e) {
            throw handle("findAll students", e);
        }
    }

    @Override
    public void update(Student student) {
        // created_at and student_id are immutable; only the mutable profile fields change.
        final String sql = "UPDATE student SET name = ?, email = ?, department = ?, phone = ? "
                + "WHERE student_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, student.getName());
            ps.setString(2, student.getEmail());
            ps.setString(3, student.getDepartment());
            ps.setString(4, student.getPhone());
            ps.setString(5, student.getStudentId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw handle("update student " + student.getStudentId(), e);
        }
    }

    @Override
    public void delete(String studentId) {
        // ON DELETE CASCADE in the schema removes the dependent wallet/membership rows.
        final String sql = "DELETE FROM student WHERE student_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw handle("delete student " + studentId, e);
        }
    }

    @Override
    public boolean exists(String studentId) {
        final String sql = "SELECT 1 FROM student WHERE student_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw handle("exists " + studentId, e);
        }
    }

    @Override
    public Optional<String> findMaxStudentId() {
        // Zero-padded ids (STU000001) sort lexicographically, so MAX gives the latest.
        final String sql = "SELECT MAX(student_id) AS max_id FROM student";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.ofNullable(rs.getString("max_id"));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handle("findMaxStudentId", e);
        }
    }

    /** Maps the current ResultSet row to a Student. Single source of row-mapping (DRY). */
    private Student mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return Student.builder()
                .studentId(rs.getString("student_id"))
                .name(rs.getString("name"))
                .email(rs.getString("email"))
                .department(rs.getString("department"))
                .phone(rs.getString("phone"))
                .createdAt(createdAt != null ? createdAt.toLocalDateTime() : null)
                .build();
    }

    /** Logs the technical cause and converts it to a uniform unchecked exception. */
    private DataAccessException handle(String operation, SQLException e) {
        Logger.error("DB error during [" + operation + "]: " + e.getMessage(), e);
        return new DataAccessException(ErrorMessages.DATABASE_ERROR, e);
    }
}
