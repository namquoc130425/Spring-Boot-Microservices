package com.nvqn.dto.request;

import com.nvqn.compare_core.constant.ErrorCode;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = ErrorCode.Key.INVALID_FORMAT)
        String email,
        @NotBlank(message = ErrorCode.Key.INVALID_FORMAT)
        String password
) {
}
