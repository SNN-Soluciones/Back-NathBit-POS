package com.snnsoluciones.backnathbitpos.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;
    
    // Constructor para Spring Data Page
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return PageResponse.<T>builder()
            .content(page.getContent())
            .pageNumber(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .empty(page.isEmpty())
            .build();
    }
    
    // Constructor para listas simples (sin paginación real)
    public static <T> PageResponse<T> of(List<T> content) {
        return PageResponse.<T>builder()
            .content(content)
            .pageNumber(0)
            .pageSize(content.size())
            .totalElements(content.size())
            .totalPages(1)
            .first(true)
            .last(true)
            .empty(content.isEmpty())
            .build();
    }
}