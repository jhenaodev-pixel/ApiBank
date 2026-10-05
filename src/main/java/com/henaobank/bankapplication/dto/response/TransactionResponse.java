package com.henaobank.bankapplication.dto.response;


import com.henaobank.bankapplication.entity.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionResponse {

    private Long id;
    private String reference;
    private String sourceAccountNumber;
    private String destinationAccount;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String description;
    private LocalDateTime timestamp;
}
