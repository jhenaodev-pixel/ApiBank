package com.henaobank.bankapplication.service.impl;

import com.henaobank.bankapplication.dto.request.LoginRequest;
import com.henaobank.bankapplication.dto.request.RegisterRequest;
import com.henaobank.bankapplication.dto.response.AuthResponse;
import com.henaobank.bankapplication.entity.Account;
import com.henaobank.bankapplication.entity.User;
import com.henaobank.bankapplication.entity.enums.AccountType;
import com.henaobank.bankapplication.entity.enums.Role;
import com.henaobank.bankapplication.exception.BadRequestException;
import com.henaobank.bankapplication.repository.AccountRepository;
import com.henaobank.bankapplication.repository.UserRepository;
import com.henaobank.bankapplication.security.JwtTokenProvider;
import com.henaobank.bankapplication.service.AuthService;
import com.henaobank.bankapplication.util.AccountNumberGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final BigDecimal WELCOME_BALANCE = new BigDecimal("1000.00");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request){
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)){
            throw new BadRequestException("El correo ya esta registrado");
        }

        User user = userRepository.save(User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .build()
        );

        accountRepository.save(Account.builder()
                .accountNumber(accountNumberGenerator.Next())
                .balance(WELCOME_BALANCE)
                .accountType(AccountType.SAVINGS)
                .user(user)
                .build());

        return AuthResponse.builder()
                .token(jwtTokenProvider.generateToken(user.getEmail()))
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request){
        String email = request.getEmail().trim().toLowerCase();

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(()-> new BadRequestException("Usuario no encontrado"));

        return AuthResponse.builder()
                .token(jwtTokenProvider.generateToken(user.getEmail()))
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
