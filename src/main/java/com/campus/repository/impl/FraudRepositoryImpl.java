package com.campus.repository.impl;

import com.campus.entity.FraudFlag;
import com.campus.repository.interfaces.IFraudRepository;
import java.util.*;

public class FraudRepositoryImpl implements IFraudRepository {
    private final Map<Long, FraudFlag> store = new HashMap<>();
    private static long idCounter = 1;

    @Override
    public void save(FraudFlag fraudFlag) {
        if (fraudFlag.getFlagId() == null) {
            fraudFlag.setFlagId(idCounter++);
        }
        store.put(fraudFlag.getFlagId(), fraudFlag);
    }

    @Override
    public Optional<FraudFlag> findById(Long flagId) {
        return Optional.ofNullable(store.get(flagId));
    }

    @Override
    public Optional<FraudFlag> findByStudentId(Long studentId) {
        return store.values().stream()
                .filter(f -> f.getStudentId().equals(studentId))
                .findFirst();
    }

    @Override
    public List<FraudFlag> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void update(FraudFlag fraudFlag) {
        if (store.containsKey(fraudFlag.getFlagId())) {
            store.put(fraudFlag.getFlagId(), fraudFlag);
        }
    }
}
