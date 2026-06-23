package com.icinfo.taskmanagement.service.news;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class FallbackNewsFetcher implements NewsFetcher {

    private final ExternalNewsSource primarySource;

    private final ExternalNewsSource fallbackSource;

    public FallbackNewsFetcher(
            @Qualifier("gdeltNewsSource") ExternalNewsSource primarySource,
            @Qualifier("googleNewsRssSource") ExternalNewsSource fallbackSource
    ) {
        this.primarySource = primarySource;
        this.fallbackSource = fallbackSource;
    }

    @Override
    public NewsFetchResult fetch(String keyword) {
        try {
            return primarySource.fetch(keyword);
        } catch (NewsFetchException primaryException) {
            try {
                return fallbackSource.fetch(keyword);
            } catch (NewsFetchException fallbackException) {
                throw new NewsFetchException("External news sources are unavailable", fallbackException);
            }
        }
    }
}
