package com.icinfo.taskmanagement.service.news;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class FallbackNewsFetcher implements NewsFetcher {

    private final List<ExternalNewsSource> sources;

    @Autowired
    public FallbackNewsFetcher(
            @Qualifier("domesticRssNewsSource") ExternalNewsSource domesticRssSource,
            @Qualifier("gdeltNewsSource") ExternalNewsSource gdeltNewsSource,
            @Qualifier("googleNewsRssSource") ExternalNewsSource googleNewsRssSource
    ) {
        this(List.of(domesticRssSource, gdeltNewsSource, googleNewsRssSource));
    }

    public FallbackNewsFetcher(ExternalNewsSource primarySource, ExternalNewsSource fallbackSource) {
        this(List.of(primarySource, fallbackSource));
    }

    FallbackNewsFetcher(List<ExternalNewsSource> sources) {
        this.sources = sources;
    }

    @Override
    public NewsFetchResult fetch(String keyword) {
        NewsFetchException lastException = null;
        for (ExternalNewsSource source : sources) {
            try {
                return source.fetch(keyword);
            } catch (NewsFetchException exception) {
                lastException = exception;
            }
        }
        throw new NewsFetchException("External news sources are unavailable", lastException);
    }
}
