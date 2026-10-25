package com.nvqn.service;

import com.nvqn.dto.request.LoginRequest;
import com.nvqn.dto.request.RegisterRequest;

public interface AuthenticationService {
    String register(RegisterRequest  registerRequest);

    String login(LoginRequest loginRequest);
}
