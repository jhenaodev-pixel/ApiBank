package com.henaobank.bankapplication.util;

import com.henaobank.bankapplication.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {
    private static final String BANK_PREFIX = "10008";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;

    public String Next() {
        String number;
        do {
            number = BANK_PREFIX + String.format("%010d", RANDOM.nextLong(10_000_000_000L));

        }while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
