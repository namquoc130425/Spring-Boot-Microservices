package com.nvqn.security_starter.security;
import tools.jackson.databind.ObjectMapper;
import com.nvqn.compare_core.constant.ErrorCode;
import com.nvqn.compare_core.dto.response.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;


import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint  implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {

        log.warn(
                "Authentication failed | method={} | path={} | message={}",
                request.getMethod(),
                request.getRequestURI(),
                authException.getMessage()
        );

        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;

        response.setStatus(errorCode.getStatus().value());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.setCharacterEncoding("UTF-8");

        APIResponse<Object> body = APIResponse.response(
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                false
                );

        objectMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}
