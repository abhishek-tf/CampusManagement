package com.campus.repository.impl;

import com.campus.entity.ExpenseGroup;
import com.campus.entity.GroupMember;
import com.campus.repository.interfaces.IExpenseRepository;
import java.util.*;
import java.util.stream.Collectors;

public class ExpenseRepositoryImpl implements IExpenseRepository {
    private final Map<Long, ExpenseGroup> groupStore = new HashMap<>();
    private final Map<Long, GroupMember> memberStore = new HashMap<>();
    private static long groupIdCounter = 1;
    private static long memberIdCounter = 1;

    @Override
    public void saveGroup(ExpenseGroup group) {
        if (group.getGroupId() == null) {
            group.setGroupId(groupIdCounter++);
        }
        groupStore.put(group.getGroupId(), group);
    }

    @Override
    public void saveMember(GroupMember member) {
        if (member.getMemberExpenseId() == null) {
            member.setMemberExpenseId(memberIdCounter++);
        }
        memberStore.put(member.getMemberExpenseId(), member);
    }

    @Override
    public Optional<ExpenseGroup> findGroupById(Long groupId) {
        return Optional.ofNullable(groupStore.get(groupId));
    }

    @Override
    public List<ExpenseGroup> findAllGroups() {
        return new ArrayList<>(groupStore.values());
    }

    @Override
    public List<GroupMember> findMembersByGroupId(Long groupId) {
        return memberStore.values().stream()
                .filter(m -> m.getGroupId().equals(groupId))
                .collect(Collectors.toList());
    }
}
