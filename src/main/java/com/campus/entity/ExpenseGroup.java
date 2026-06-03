package com.campus.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps the {@code expense_group} table: a named group of students who share
 * expenses. {@code createdBy} is the {@code student_id} (VARCHAR) of the creator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseGroup {
    private Long groupId;
    private String groupName;
    private String createdBy;
    private LocalDateTime createdAt;
}
