package com.campus.entity;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@ToString(exclude = {"createdAt", "updatedAt"})
public class Student {
    private Long studentId;
    private String name;
    private String email;
    private String phone;
    private String department;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
