package com.longobuccodev.app_adm_obras.core.repository;

import java.util.List;

public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public Page {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
