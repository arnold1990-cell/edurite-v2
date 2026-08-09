package com.edurite.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageRequestUtils {
    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final int MAX_PAGE_SIZE = 100;

    private PageRequestUtils() {
    }

    public static PageRequest capped(int page, int size) {
        return PageRequest.of(normalizePage(page), capSize(size));
    }

    public static PageRequest capped(int page, int size, Sort sort) {
        return PageRequest.of(normalizePage(page), capSize(size), sort);
    }

    public static int normalizePage(int page) {
        return Math.max(0, page);
    }

    public static int capSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
