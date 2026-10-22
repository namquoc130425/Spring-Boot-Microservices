package com.nvqn.compare_core.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;


    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum ErrorCode {
        VALIDATION_ERROR("Validation error", HttpStatus.BAD_REQUEST),
        DATA_INVALID("Invalid data", HttpStatus.BAD_REQUEST),
        UNAUTHENTICATED("Unauthenticated", HttpStatus.UNAUTHORIZED),
        INVALID_TOKEN("Invalid token", HttpStatus.UNAUTHORIZED),
        FORBIDDEN("You do not have permission", HttpStatus.FORBIDDEN),
        NOT_FOUND("Not found", HttpStatus.NOT_FOUND),
        CONFLICT("Conflict", HttpStatus.CONFLICT),
        ACTIVE_SUBSCRIPTION_EXISTS("An active Premium subscription already exists", HttpStatus.CONFLICT),
        TOO_MANY_REQUESTS("Too many requests", HttpStatus.TOO_MANY_REQUESTS),
        SERVICE_UNAVAILABLE("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
        INVALID_ENUM("Invalid enum", HttpStatus.BAD_REQUEST),
        INVALID_FORMAT("Invalid format", HttpStatus.BAD_REQUEST),
        UNCATEGORIZED_EXCEPTION("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

        String message;
        HttpStatus status;

        public int getCode() { return status.value(); }
        public static final class Key {
            public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
            public static final String DATA_INVALID = "DATA_INVALID";
            public static final String INVALID_ENUM = "INVALID_ENUM";
            public static final String INVALID_FORMAT = "INVALID_FORMAT";

            private Key() {
            }
        }
    }
