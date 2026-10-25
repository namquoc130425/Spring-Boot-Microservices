package com.nvqn.dto.request;

import com.nvqn.compare_core.constant.ErrorCode;
import com.nvqn.compare_core.validator.EnumValidator;
import com.nvqn.constant.Role;
import jakarta.validation.constraints.NotBlank;


public record TokenPrincipal(
        @NotBlank(message = ErrorCode.Key.DATA_INVALID)
        String userCode,
        @NotBlank(message = ErrorCode.Key.DATA_INVALID)
        String username,
        @NotBlank(message = ErrorCode.Key.DATA_INVALID)
        @EnumValidator(enumClass = Role.class)
        String role
) {
}
