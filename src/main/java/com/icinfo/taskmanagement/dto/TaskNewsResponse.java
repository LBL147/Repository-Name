package com.icinfo.taskmanagement.dto;

import com.icinfo.taskmanagement.entity.NewsItem;
import java.time.LocalDateTime;

public class TaskNewsResponse {

    private Long id;

    private Long newsId;

    private String title;

    private String url;

    private String source;

    private String keyword;

    private LocalDateTime publishedAt;

    private LocalDateTime fetchedAt;

    private LocalDateTime associatedAt;

    public static TaskNewsResponse from(Long id, NewsItem newsItem, LocalDateTime associatedAt) {
        TaskNewsResponse response = new TaskNewsResponse();
        response.setId(id);
        response.setNewsId(newsItem.getId());
        response.setTitle(newsItem.getTitle());
        response.setUrl(newsItem.getUrl());
        response.setSource(newsItem.getSource());
        response.setKeyword(newsItem.getKeyword());
        response.setPublishedAt(newsItem.getPublishedAt());
        response.setFetchedAt(newsItem.getFetchedAt());
        response.setAssociatedAt(associatedAt);
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
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

    public LocalDateTime getAssociatedAt() {
        return associatedAt;
    }

    public void setAssociatedAt(LocalDateTime associatedAt) {
        this.associatedAt = associatedAt;
    }
}
