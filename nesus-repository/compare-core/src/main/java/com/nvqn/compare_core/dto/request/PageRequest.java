package com.nvqn.compare_core.dto.request;

import com.nvqn.compare_core.constant.SortDirection;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageRequest {
    @Builder.Default int page = 0;
    @Builder.Default int size = 10;
    @Builder.Default String sortBy = "createdAt";
    @Builder.Default SortDirection direction = SortDirection.DESC;
    String search;
}