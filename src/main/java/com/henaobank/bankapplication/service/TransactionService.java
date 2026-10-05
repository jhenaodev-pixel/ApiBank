package com.henaobank.bankapplication.service;

import com.henaobank.bankapplication.dto.request.MoneyOperationRequest;
import com.henaobank.bankapplication.dto.request.TransferRequest;
import com.henaobank.bankapplication.dto.response.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse transfer(TransferRequest request, String email);
    TransactionResponse deposit(MoneyOperationRequest request, String email);
    TransactionResponse withdraw(MoneyOperationRequest request, String email);
    List<TransactionResponse> getAccountHistory(String accountNumber, String email);
}
