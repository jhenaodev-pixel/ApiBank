package com.henaobank.bankapplication.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.henaobank.bankapplication.dto.request.MoneyOperationRequest;
import com.henaobank.bankapplication.dto.request.TransferRequest;
import com.henaobank.bankapplication.dto.response.TransactionResponse;
import com.henaobank.bankapplication.entity.Account;
import com.henaobank.bankapplication.entity.Transaction;
import com.henaobank.bankapplication.entity.enums.TransactionType;
import com.henaobank.bankapplication.exception.BadRequestException;
import com.henaobank.bankapplication.exception.InsufficientBalanceException;
import com.henaobank.bankapplication.exception.ResourceNotFoundException;
import com.henaobank.bankapplication.repository.AccountRepository;
import com.henaobank.bankapplication.repository.TransactionRepository;
import com.henaobank.bankapplication.service.TransactionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public TransactionResponse transfer(TransferRequest request, String email) {
        String sourceNumber = request.getSourceAccountNumber();
        String destinationNumber = request.getDestinationAccountNumber();

        // 1. Origen y destino deben ser distintos
        if (sourceNumber.equals(destinationNumber)) {
            throw new BadRequestException("La cuenta de origen y la de destino no pueden ser la misma");
        }

        // 2. Bloqueamos ambas cuentas siempre en el mismo orden (alfabético) para evitar deadlocks
        Map<String, Account> locked = new HashMap<>();
        for (String number : Stream.of(sourceNumber, destinationNumber).sorted().toList()) {
            locked.put(number, findAccountForUpdate(number));
        }
        Account source = locked.get(sourceNumber);
        Account destination = locked.get(destinationNumber);

        // 3. La cuenta de origen debe ser del usuario que hace la operación
        assertOwner(source, email);

        // 4. Restamos (valida saldo) y sumamos
        debit(source, request.getAmount());
        destination.setBalance(destination.getBalance().add(request.getAmount()));

        // 5. Registramos el movimiento
        return mapToResponse(save(source, destination, request.getAmount(),
                TransactionType.TRANSFER, request.getDescription()));
    }

    @Override
    @Transactional
    public TransactionResponse deposit(MoneyOperationRequest request, String email) {
        Account account = findAccountForUpdate(request.getAccountNumber());
        assertOwner(account, email);

        account.setBalance(account.getBalance().add(request.getAmount()));

        return mapToResponse(save(null, account, request.getAmount(),
                TransactionType.DEPOSIT, request.getDescription()));
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(MoneyOperationRequest request, String email) {
        Account account = findAccountForUpdate(request.getAccountNumber());
        assertOwner(account, email);

        debit(account, request.getAmount());

        return mapToResponse(save(account, null, request.getAmount(),
                TransactionType.WITHDRAWAL, request.getDescription()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountHistory(String accountNumber, String email) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> notFound(accountNumber));
        assertOwner(account, email);

        return transactionRepository.findAllByAccountId(account.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ---------------------------------------------------------- métodos auxiliares

    private Account findAccountForUpdate(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> notFound(accountNumber));
    }

    /** Si la cuenta es de otra persona, respondemos igual que si no existiera (404). */
    private void assertOwner(Account account, String email) {
        if (!account.getUser().getEmail().equalsIgnoreCase(email)) {
            throw notFound(account.getAccountNumber());
        }
    }

    private void debit(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente en la cuenta " + account.getAccountNumber());
        }
        account.setBalance(account.getBalance().subtract(amount));
    }

    private Transaction save(Account source, Account destination, BigDecimal amount,
                             TransactionType type, String description) {
        return transactionRepository.save(Transaction.builder()
                .reference(UUID.randomUUID().toString())
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(amount)
                .transactionType(type)
                .description(description)
                .build());
    }

    private ResourceNotFoundException notFound(String accountNumber) {
        return new ResourceNotFoundException("Cuenta no encontrada: " + accountNumber);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .reference(t.getReference())
                .sourceAccountNumber(t.getSourceAccount() != null ? t.getSourceAccount().getAccountNumber() : null)
                .destinationAccount(t.getDestinationAccount() != null ? t.getDestinationAccount().getAccountNumber() : null)
                .amount(t.getAmount())
                .transactionType(t.getTransactionType())
                .description(t.getDescription())
                .timestamp(t.getTimestamp())
                .build();
    }
}