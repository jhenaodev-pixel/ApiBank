package com.henaobank.bankapplication.controller;

import com.henaobank.bankapplication.dto.request.CreateAccountRequest;
import com.henaobank.bankapplication.dto.response.AccountResponse;
import com.henaobank.bankapplication.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        return ResponseEntity.ok(accountService.getUserAccounts(authentication.getName()));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber,
                                                              Authentication authentication) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request.getAccountType(), authentication.getName()));
    }
}
