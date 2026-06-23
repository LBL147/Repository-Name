package com.icinfo.taskmanagement.common;

import java.util.List;

public class PageResponse<T> {

    private final List<T> records;

    private final long total;

    private final long page;

    private final long size;

    public PageResponse(List<T> records, long total, long page, long size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<T> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public long getPage() {
        return page;
    }

    public long getSize() {
        return size;
    }
}
