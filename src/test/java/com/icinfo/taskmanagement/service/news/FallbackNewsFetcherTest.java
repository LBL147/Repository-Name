package com.icinfo.taskmanagement.service.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class FallbackNewsFetcherTest {

    @Test
    void usesPrimarySourceWhenPrimarySucceeds() {
        ExternalNewsSource primary = mock(ExternalNewsSource.class);
        ExternalNewsSource fallback = mock(ExternalNewsSource.class);
        NewsFetchResult primaryResult = new NewsFetchResult(
                "GDELT DOC API",
                List.of(new ExternalNewsItem("Title", "https://example.com/news", "Example", null)));
        when(primary.fetch("AI")).thenReturn(primaryResult);

        NewsFetchResult result = new FallbackNewsFetcher(primary, fallback).fetch("AI");

        assertThat(result).isSameAs(primaryResult);
        verify(primary).fetch("AI");
        verifyNoInteractions(fallback);
    }

    @Test
    void usesFallbackSourceWhenPrimaryFails() {
        ExternalNewsSource primary = mock(ExternalNewsSource.class);
        ExternalNewsSource fallback = mock(ExternalNewsSource.class);
        NewsFetchResult fallbackResult = new NewsFetchResult(
                "Google News RSS",
                List.of(new ExternalNewsItem("Title", "https://example.com/rss", "Example", null)));
        when(primary.fetch("AI")).thenThrow(new NewsFetchException("primary down"));
        when(fallback.fetch("AI")).thenReturn(fallbackResult);

        NewsFetchResult result = new FallbackNewsFetcher(primary, fallback).fetch("AI");

        assertThat(result).isSameAs(fallbackResult);
        verify(primary).fetch("AI");
        verify(fallback).fetch("AI");
    }

    @Test
    void throwsWhenBothSourcesFail() {
        ExternalNewsSource primary = mock(ExternalNewsSource.class);
        ExternalNewsSource fallback = mock(ExternalNewsSource.class);
        when(primary.fetch("AI")).thenThrow(new NewsFetchException("primary down"));
        when(fallback.fetch("AI")).thenThrow(new NewsFetchException("fallback down"));

        assertThatThrownBy(() -> new FallbackNewsFetcher(primary, fallback).fetch("AI"))
                .isInstanceOf(NewsFetchException.class)
                .hasMessage("External news sources are unavailable");
    }
}
