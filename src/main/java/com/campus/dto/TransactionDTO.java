package com.campus.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;


public class TransactionDTO {
    private Long transactionId;
    private Long studentId;
    private String type;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
