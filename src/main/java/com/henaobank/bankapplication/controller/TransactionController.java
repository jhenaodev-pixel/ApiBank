package com.henaobank.bankapplication.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.henaobank.bankapplication.dto.request.MoneyOperationRequest;
import com.henaobank.bankapplication.dto.request.TransferRequest;
import com.henaobank.bankapplication.dto.response.TransactionResponse;
import com.henaobank.bankapplication.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(request, authentication.getName()));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody MoneyOperationRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.deposit(request, authentication.getName()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody MoneyOperationRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.withdraw(request, authentication.getName()));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> history(@PathVariable String accountNumber,
                                                             Authentication authentication) {
        return ResponseEntity.ok(transactionService.getAccountHistory(accountNumber, authentication.getName()));
    }
}