package com.nvqn.compare_core.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidatorClass
        implements ConstraintValidator<EnumValidator, String> {

    // Danh sách các giá trị hợp lệ của Enum
    private Set<String> allowedValues;

    // 1. Chạy khi validator được khởi tạo
    // Lấy tất cả giá trị Enum và lưu vào allowedValues
    @Override
    public void initialize(EnumValidator constraintAnnotation) {

        allowedValues = Arrays.stream(
                        constraintAnnotation.enumClass().getEnumConstants()
                )
                .map(Enum::name) // Enum -> tên String
                .collect(Collectors.toUnmodifiableSet());
    }

    // 2. Chạy khi dữ liệu được gửi vào để validate
    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext context) {

        // Null/rỗng để @NotNull hoặc @NotBlank xử lý
        if (value == null || value.isBlank()) {
            return true;
        }

        // 3. Chuẩn hóa dữ liệu rồi kiểm tra có nằm trong Enum không
        return allowedValues.contains(
                value.trim().toUpperCase(Locale.ROOT)
        );
    }
}