package com.icinfo.taskmanagement.dto;

import java.util.List;

public class RefreshNewsResponse {

    private List<NewsItemResponse> records;

    private int fetchedCount;

    private int insertedCount;

    private String source;

    private boolean cacheFallback;

    private String message;

    public RefreshNewsResponse(
            List<NewsItemResponse> records,
            int fetchedCount,
            int insertedCount,
            String source,
            boolean cacheFallback,
            String message
    ) {
        this.records = records;
        this.fetchedCount = fetchedCount;
        this.insertedCount = insertedCount;
        this.source = source;
        this.cacheFallback = cacheFallback;
        this.message = message;
    }

    public List<NewsItemResponse> getRecords() {
        return records;
    }

    public void setRecords(List<NewsItemResponse> records) {
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
