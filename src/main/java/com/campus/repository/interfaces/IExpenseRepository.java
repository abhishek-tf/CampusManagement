package com.campus.repository.interfaces;

import com.campus.entity.ExpenseGroup;
import com.campus.entity.GroupMember;
import java.util.Optional;
import java.util.List;

public interface IExpenseRepository {
    void saveGroup(ExpenseGroup group);
    void saveMember(GroupMember member);
    Optional<ExpenseGroup> findGroupById(Long groupId);
    List<ExpenseGroup> findAllGroups();
    List<GroupMember> findMembersByGroupId(Long groupId);
}
