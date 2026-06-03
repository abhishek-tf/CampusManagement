package com.campus.repository.interfaces;

import com.campus.entity.FraudFlag;
import java.util.Optional;
import java.util.List;

public interface IFraudRepository {
    void save(FraudFlag fraudFlag);
    Optional<FraudFlag> findById(Long flagId);
    Optional<FraudFlag> findByStudentId(Long studentId);
    List<FraudFlag> findAll();
    void update(FraudFlag fraudFlag);
}
