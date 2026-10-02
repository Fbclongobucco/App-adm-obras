package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.core.repository.Page;

import java.util.function.Function;

public final class PageMapper {

    private PageMapper() {
    }

    public static <T> PageResponseDTO<T> toResponse(Page<T> page) {
        return toResponse(page, Function.identity());
    }

    public static <S, T> PageResponseDTO<T> toResponse(Page<S> page, Function<S, T> mapper) {
        if (page == null) {
            return null;
        }
        return new PageResponseDTO<>(
                page.content().stream().map(mapper).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                !page.hasPrevious(),
                !page.hasNext()
        );
    }
}
