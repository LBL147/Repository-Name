package com.icinfo.taskmanagement.dto;

import com.icinfo.taskmanagement.entity.NewsItem;
import java.time.LocalDateTime;

public class NewsItemResponse {

    private Long id;

    private String title;

    private String url;

    private String source;

    private String keyword;

    private LocalDateTime publishedAt;

    private LocalDateTime fetchedAt;

    public static NewsItemResponse from(NewsItem newsItem) {
        NewsItemResponse response = new NewsItemResponse();
        response.setId(newsItem.getId());
        response.setTitle(newsItem.getTitle());
        response.setUrl(newsItem.getUrl());
        response.setSource(newsItem.getSource());
        response.setKeyword(newsItem.getKeyword());
        response.setPublishedAt(newsItem.getPublishedAt());
        response.setFetchedAt(newsItem.getFetchedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}
