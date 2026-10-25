package com.nvqn.service;

import com.nvqn.dto.request.TokenPrincipal;

public interface TokenService {
    String generateAccessToken(TokenPrincipal tokenPrincipal);
}
