package com.campus.service.interfaces;

import com.campus.exception.CampusPaymentException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IExpenseService {
    void createExpenseGroup(Long createdBy, String groupName, String description) throws CampusPaymentException;
    void addMemberToGroup(Long groupId, Long studentId) throws CampusPaymentException;
    List<Map<String, Object>> calculateSettlements(Long groupId) throws CampusPaymentException;
}
