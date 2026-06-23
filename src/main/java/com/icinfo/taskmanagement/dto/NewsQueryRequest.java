package com.icinfo.taskmanagement.dto;

public class NewsQueryRequest {

    private String keyword;

    private Long page = 1L;

    private Long size = 10L;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
