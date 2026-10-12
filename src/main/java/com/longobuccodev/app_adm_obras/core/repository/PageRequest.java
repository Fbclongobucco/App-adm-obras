package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.exception.InvalidPageException;

public record PageRequest(int page, int size, Sort sort) {

    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 20;

    public PageRequest {
        if (page < 0) {
            throw InvalidPageException.negativePage(page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw InvalidPageException.invalidSize(size);
        }
    }

    public PageRequest(int page, int size) {
        this(page, size, null);
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }

    public static PageRequest firstPage() {
        return new PageRequest(0, DEFAULT_SIZE);
    }

    public static PageRequest ofSize(int size) {
        return new PageRequest(0, size);
    }

    public PageRequest withSort(Sort sort) {
        return new PageRequest(page, size, sort);
    }

    public int offset() {
        return page * size;
    }
}
