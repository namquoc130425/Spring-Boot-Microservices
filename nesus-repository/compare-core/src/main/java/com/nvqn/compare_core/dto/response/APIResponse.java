package com.nvqn.compare_core.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResponse<T> {
    int code;
    String message;
    T result;
    boolean success;

    public static <T> APIResponse<T> response(int code, String message, T result, boolean success) {
        return APIResponse.<T>builder()
                .code(code)
                .message(message)
                .result(result)
                .success(success)
                .build();
    }

    public static <T> APIResponse<T> success(T result) {
        return APIResponse.<T>builder()
                .code(200)
                .message("Success")
                .result(result)
                .success(true)
                .build();
    }

}
