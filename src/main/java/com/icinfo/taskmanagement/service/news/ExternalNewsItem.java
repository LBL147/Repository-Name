package com.icinfo.taskmanagement.service.news;

import java.time.LocalDateTime;

public class ExternalNewsItem {

    private final String title;

    private final String url;

    private final String source;

    private final LocalDateTime publishedAt;

    private final String description;

    public ExternalNewsItem(String title, String url, String source, LocalDateTime publishedAt) {
        this(title, url, source, publishedAt, null);
    }

    public ExternalNewsItem(
            String title,
            String url,
            String source,
            LocalDateTime publishedAt,
            String description
    ) {
        this.title = title;
        this.url = url;
        this.source = source;
        this.publishedAt = publishedAt;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getDescription() {
        return description;
    }
}
