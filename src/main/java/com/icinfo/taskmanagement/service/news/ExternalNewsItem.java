package com.icinfo.taskmanagement.service.news;

import java.time.LocalDateTime;

public class ExternalNewsItem {

    private final String title;

    private final String url;

    private final String source;

    private final LocalDateTime publishedAt;

    public ExternalNewsItem(String title, String url, String source, LocalDateTime publishedAt) {
        this.title = title;
        this.url = url;
        this.source = source;
        this.publishedAt = publishedAt;
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
}
