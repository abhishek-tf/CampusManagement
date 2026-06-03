package com.campus.service.impl;

import com.campus.entity.ExpenseGroup;
import com.campus.entity.GroupMember;
import com.campus.exception.CampusPaymentException;
import com.campus.repository.interfaces.IExpenseRepository;
import com.campus.service.interfaces.IExpenseService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class ExpenseServiceImpl implements IExpenseService {
    private final IExpenseRepository expenseRepository;

    public ExpenseServiceImpl(IExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public void createExpenseGroup(Long createdBy, String groupName, String description) throws CampusPaymentException {
        if (createdBy == null || groupName == null || groupName.isEmpty()) {
            throw new CampusPaymentException("Invalid group data");
        }

        ExpenseGroup group = ExpenseGroup.builder()
                .createdByStudentId(createdBy)
                .groupName(groupName)
                .description(description)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        expenseRepository.saveGroup(group);
    }

    @Override
    public void addMemberToGroup(Long groupId, Long studentId) throws CampusPaymentException {
        if (groupId == null || studentId == null) {
            throw new CampusPaymentException("Invalid group or student ID");
        }

        GroupMember member = GroupMember.builder()
                .groupId(groupId)
                .studentId(studentId)
                .isPaid(false)
                .build();

        expenseRepository.saveMember(member);
    }

    @Override
    public List<Map<String, Object>> calculateSettlements(Long groupId) throws CampusPaymentException {
        if (groupId == null) {
            throw new CampusPaymentException("Invalid group ID");
        }

        List<GroupMember> members = expenseRepository.findMembersByGroupId(groupId);
        List<Map<String, Object>> settlements = new ArrayList<>();

        if (members.size() <= 1) {
            return settlements;
        }

        BigDecimal perPersonAmount = BigDecimal.valueOf(100);

        for (int i = 0; i < members.size() - 1; i++) {
            Map<String, Object> settlement = new LinkedHashMap<>();
            settlement.put("fromStudentId", members.get(i).getStudentId());
            settlement.put("toStudentId", members.get(i + 1).getStudentId());
            settlement.put("amount", perPersonAmount);
            settlements.add(settlement);
        }

        return settlements;
    }
}
