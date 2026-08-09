package com.edurite.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageRequestUtilsTest {

    @Test
    void cappedNormalizesNegativePagesAndCapsOversizedRequests() {
        var request = PageRequestUtils.capped(-5, 1_000_000);

        assertThat(request.getPageNumber()).isZero();
        assertThat(request.getPageSize()).isEqualTo(PageRequestUtils.MAX_PAGE_SIZE);
    }

    @Test
    void cappedUsesDefaultSizeForInvalidSizes() {
        var request = PageRequestUtils.capped(2, 0);

        assertThat(request.getPageNumber()).isEqualTo(2);
        assertThat(request.getPageSize()).isEqualTo(PageRequestUtils.DEFAULT_PAGE_SIZE);
    }
}
