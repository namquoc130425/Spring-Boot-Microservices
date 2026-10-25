package com.nvqn.security_starter.security;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Validates the audience (aud) claim of a JWT.

 * Purpose:
 * Ensure the JWT is intended for this Resource Server.

 * How it works:
 * 1. Read the "aud" claim from JWT.
 * 2. Compare it with configured allowed audiences.
 * 3. If at least one audience matches -> SUCCESS.
 * 4. Otherwise -> FAILURE (invalid_token).

 * Why needed:
 * A valid JWT signature does not guarantee that the
 * token was issued for this specific microservice.

 * Example:
 * Allowed audience: ["payment-service"]
 * JWT aud: ["order-service"] -> REJECTED
 * JWT aud: ["payment-service"] -> ACCEPTED

 * Note:
 * This validator does NOT validate JWT signatures,
 * expiration, roles, or permissions.
 */

// class nay dùng để kiểm tra token có được phép sử dụng trong service hiện tại không
public final class AudienceValidator implements OAuth2TokenValidator<Jwt>{

    // OAuth2TokenValidator<Jwt> interface của spring security dùng để kiểm tra JWT có đáp ứng yêu cầu hợp lệ hay không

    private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(  // lỗi đc trả về nếu token không chứa audience hợp lệ
            "invalid_token",
            "JWT audience is not allowed for this service",
            null
    );

    private final List<String> requiredAudiences; // dánh sách các audience hợp lệ mà service này chấp nhận

    public AudienceValidator(List<String> requiredAudiences) {
        if (requiredAudiences == null || requiredAudiences.isEmpty()) {
            throw new IllegalArgumentException("requiredAudiences must not be empty");
        }
        this.requiredAudiences = List.copyOf(requiredAudiences); // sao chép danh sách bằng List.copyOf tạo bản sao không thể chỉnh sửa
    }
    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        boolean accepted = token.getAudience() != null && token.getAudience().stream().anyMatch(requiredAudiences::contains);
        //Chỉ cần ít nhất một audience trong token trùng với audience được service cho phép thì kết quả là true.

        return accepted ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
    }
}
