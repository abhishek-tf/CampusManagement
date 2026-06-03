package com.campus.entity;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@ToString(exclude = {"createdAt", "updatedAt"})
public class ExpenseGroup {
    private Long groupId;
    private Long createdByStudentId;
    private String groupName;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
