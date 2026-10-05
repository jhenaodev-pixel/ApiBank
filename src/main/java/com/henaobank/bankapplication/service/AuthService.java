package com.henaobank.bankapplication.service;

import com.henaobank.bankapplication.dto.request.LoginRequest;
import com.henaobank.bankapplication.dto.request.RegisterRequest;
import com.henaobank.bankapplication.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register (RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
