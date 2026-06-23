package com.icinfo.taskmanagement.service.news;

import java.util.List;

public class NewsFetchResult {

    private final String source;

    private final List<ExternalNewsItem> items;

    public NewsFetchResult(String source, List<ExternalNewsItem> items) {
        this.source = source;
        this.items = items;
    }

    public String getSource() {
        return source;
    }

    public List<ExternalNewsItem> getItems() {
        return items;
    }
}
