package com.nvqn.compare_core.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;


import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagesResponse<T> {
    List<T> data;
    MetaData metadata;

    public static <T> PagesResponse<T> fromPage(Page<T> page) {
        return PagesResponse.<T>builder()
                .data(page.getContent())
                .metadata(MetaData.builder()
                        .pages(page.getTotalPages())
                        .size(page.getSize())
                        .currentElements(page.getNumberOfElements())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .hasNext(page.hasNext())
                        .hasPrevious(page.hasPrevious())
                        .build()
                )
                .build();
    }
}
