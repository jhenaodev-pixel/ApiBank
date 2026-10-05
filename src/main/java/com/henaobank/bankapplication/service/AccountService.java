package com.henaobank.bankapplication.service;

import com.henaobank.bankapplication.dto.response.AccountResponse;
import com.henaobank.bankapplication.entity.Account;
import com.henaobank.bankapplication.entity.enums.AccountType;

import java.util.List;

public interface AccountService {

    List<AccountResponse> getUserAccounts (String email);
    AccountResponse getAccountByNumber (String accountNumber, String email);
    AccountResponse createAccount (AccountType accountType, String email);
}
