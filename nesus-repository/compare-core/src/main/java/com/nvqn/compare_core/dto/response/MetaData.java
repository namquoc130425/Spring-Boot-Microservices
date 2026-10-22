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
public class MetaData {
    int pages;
    int size;
    int currentElements;
    long totalElements;
    int totalPages;
    boolean hasNext;
    boolean hasPrevious;
}
