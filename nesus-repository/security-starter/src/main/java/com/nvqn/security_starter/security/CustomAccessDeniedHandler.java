package com.nvqn.security_starter.security;

import tools.jackson.databind.ObjectMapper;
import com.nvqn.compare_core.constant.ErrorCode;
import com.nvqn.compare_core.dto.response.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler  implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {

        log.warn("Access denied | method={} | path={} | message={}",
                request.getMethod(),
                request.getRequestURI(),
                accessDeniedException.getMessage()
        );

        ErrorCode errorCode = ErrorCode.FORBIDDEN;

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
