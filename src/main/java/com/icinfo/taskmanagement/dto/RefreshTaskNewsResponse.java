package com.icinfo.taskmanagement.dto;

import java.util.List;

public class RefreshTaskNewsResponse {

    private String keyword;

    private List<TaskNewsResponse> records;

    private int fetchedCount;

    private int insertedCount;

    private int associatedCount;

    private String source;

    private boolean cacheFallback;

    private boolean refreshSucceeded;

    private String message;

    public RefreshTaskNewsResponse(
            String keyword,
            List<TaskNewsResponse> records,
            int fetchedCount,
            int insertedCount,
            int associatedCount,
            String source,
            boolean cacheFallback,
            boolean refreshSucceeded,
            String message
    ) {
        this.keyword = keyword;
        this.records = records;
        this.fetchedCount = fetchedCount;
        this.insertedCount = insertedCount;
        this.associatedCount = associatedCount;
        this.source = source;
        this.cacheFallback = cacheFallback;
        this.refreshSucceeded = refreshSucceeded;
        this.message = message;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<TaskNewsResponse> getRecords() {
        return records;
    }

    public void setRecords(List<TaskNewsResponse> records) {
        this.records = records;
    }

    public int getFetchedCount() {
        return fetchedCount;
    }

    public void setFetchedCount(int fetchedCount) {
        this.fetchedCount = fetchedCount;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public void setInsertedCount(int insertedCount) {
        this.insertedCount = insertedCount;
    }

    public int getAssociatedCount() {
        return associatedCount;
    }

    public void setAssociatedCount(int associatedCount) {
        this.associatedCount = associatedCount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isCacheFallback() {
        return cacheFallback;
    }

    public void setCacheFallback(boolean cacheFallback) {
        this.cacheFallback = cacheFallback;
    }

    public boolean isRefreshSucceeded() {
        return refreshSucceeded;
    }

    public void setRefreshSucceeded(boolean refreshSucceeded) {
        this.refreshSucceeded = refreshSucceeded;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
