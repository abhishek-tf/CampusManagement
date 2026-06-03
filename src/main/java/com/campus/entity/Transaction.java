package com.campus.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private Long txnId;
    private Long walletId;
    private String txnType;
    private Double amount;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
}