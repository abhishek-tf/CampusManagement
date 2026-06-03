package com.campus.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps the {@code group_member} table: membership of a student in a group.
 * The natural key is the ({@code groupId}, {@code studentId}) pair.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {
    private Long groupId;
    private String studentId;
    private LocalDateTime joinedAt;
}
