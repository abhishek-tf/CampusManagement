package com.campus.repository.interfaces;

import com.campus.entity.TransferTransaction;
import java.util.Optional;
import java.util.List;

public interface ITransferRepository {
    void save(TransferTransaction transfer);
    Optional<TransferTransaction> findById(Long transferId);
    List<TransferTransaction> findByFromStudentId(Long studentId);
    List<TransferTransaction> findByToStudentId(Long studentId);
    List<TransferTransaction> findAll();
}
