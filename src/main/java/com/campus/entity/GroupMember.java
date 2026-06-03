package com.campus.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class GroupMember {
    private Long memberExpenseId;
    private Long groupId;
    private Long studentId;
    private Boolean isPaid;
}
