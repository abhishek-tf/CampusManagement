package com.campus.repository.impl;

import com.campus.entity.ExpenseSplits;
import com.campus.repository.interfaces.ISplitRepository;
import java.util.*;
import java.util.stream.Collectors;

public class SplitRepositoryImpl implements ISplitRepository {
    private final Map<Long, ExpenseSplits> store = new HashMap<>();
    private static long idCounter = 1;

    @Override
    public void save(ExpenseSplits split) {
        if (split.getSplitId() == null) {
            split.setSplitId(idCounter++);
        }
        store.put(split.getSplitId(), split);
    }

    @Override
    public Optional<ExpenseSplits> findById(Long splitId) {
        return Optional.ofNullable(store.get(splitId));
    }

    @Override
    public List<ExpenseSplits> findByGroupId(Long groupId) {
        return store.values().stream()
                .filter(s -> s.getGroupId().equals(groupId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpenseSplits> findByStudentId(Long studentId) {
        return store.values().stream()
                .filter(s -> s.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    @Override
    public void update(ExpenseSplits split) {
        if (store.containsKey(split.getSplitId())) {
            store.put(split.getSplitId(), split);
        }
    }
}
