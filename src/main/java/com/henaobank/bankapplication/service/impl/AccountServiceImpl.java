package com.henaobank.bankapplication.service.impl;

import com.henaobank.bankapplication.dto.response.AccountResponse;
import com.henaobank.bankapplication.entity.Account;
import com.henaobank.bankapplication.entity.User;
import com.henaobank.bankapplication.entity.enums.AccountType;
import com.henaobank.bankapplication.exception.ResourceNotFoundException;
import com.henaobank.bankapplication.repository.AccountRepository;
import com.henaobank.bankapplication.repository.UserRepository;
import com.henaobank.bankapplication.service.AccountService;
import com.henaobank.bankapplication.util.AccountNumberGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    @Transactional
    public List<AccountResponse> getUserAccounts(String email) {
        User user = findUser(email);
        return accountRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public AccountResponse getAccountByNumber(String accountNumber, String email) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .filter(a -> a.getUser().getEmail().equalsIgnoreCase(email))
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada: " + accountNumber));
        return mapToResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse createAccount(AccountType accountType, String email) {
        User user = findUser(email);
        Account account = Account.builder()
                .accountNumber(accountNumberGenerator.Next())
                .balance(BigDecimal.ZERO.setScale(2))
                .accountType(accountType)
                .user(user)
                .build();
        return mapToResponse(accountRepository.save(account));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el email: " + email));
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .build();
    }
}
