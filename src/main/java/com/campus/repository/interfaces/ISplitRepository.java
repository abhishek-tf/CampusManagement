package com.campus.repository.interfaces;

import com.campus.entity.ExpenseSplits;
import java.util.Optional;
import java.util.List;

public interface ISplitRepository {
    void save(ExpenseSplits split);
    Optional<ExpenseSplits> findById(Long splitId);
    List<ExpenseSplits> findByGroupId(Long groupId);
    List<ExpenseSplits> findByStudentId(Long studentId);
    void update(ExpenseSplits split);
}
