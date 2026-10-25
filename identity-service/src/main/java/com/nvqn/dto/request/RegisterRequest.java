package com.nvqn.dto.request;

import com.nvqn.compare_core.constant.ErrorCode;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RegisterRequest(
        @NotBlank(message = ErrorCode.Key.INVALID_FORMAT)
        String phone,
        @NotBlank(message = ErrorCode.Key.INVALID_FORMAT)
        String email,
        @NotNull(message = ErrorCode.Key.INVALID_FORMAT)
        LocalDateTime dateOfBirth,
        @NotBlank(message = ErrorCode.Key.INVALID_FORMAT)
        String password
) {
}
