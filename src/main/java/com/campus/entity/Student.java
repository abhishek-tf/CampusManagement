package com.campus.entity;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Student entity.
 *
 * WHY  : Mirrors the `student` table in schema.sql 1:1 — it is the campus
 *        identity record every wallet, payment and transfer points at.
 * HOW  : Pure data holder. Lombok generates getters/setters/builder/toString,
 *        so the class carries no business logic (entities must not, per CLAUDE.md).
 * USED BY : StudentRepositoryImpl (row <-> object mapping),
 *           StudentServiceImpl (business operations),
 *           StudentMenu (display).
 *
 * Schema mapping (source of truth = schema.sql):
 *   student_id VARCHAR(20) PK   -> studentId  (String, app-generated e.g. STU000001)
 *   name       VARCHAR(100)     -> name
 *   email      VARCHAR(150) UQ  -> email
 *   department VARCHAR(60)      -> department
 *   phone      VARCHAR(15) NULL -> phone
 *   created_at DATETIME         -> createdAt
 * NOTE: the `student` table has NO updated_at column, so neither does this entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@ToString(exclude = {"createdAt"})
public class Student {
    /** Business primary key (VARCHAR(20)); never the email. */
    private String studentId;
    private String name;
    private String email;
    private String department;
    private String phone;
    private LocalDateTime createdAt;
}
