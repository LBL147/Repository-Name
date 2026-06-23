package com.icinfo.taskmanagement.controller;

import com.icinfo.taskmanagement.common.ApiResponse;
import com.icinfo.taskmanagement.common.PageResponse;
import com.icinfo.taskmanagement.dto.NewsItemResponse;
import com.icinfo.taskmanagement.dto.NewsQueryRequest;
import com.icinfo.taskmanagement.dto.RefreshNewsRequest;
import com.icinfo.taskmanagement.dto.RefreshNewsResponse;
import com.icinfo.taskmanagement.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NewsItemResponse>> listNews(NewsQueryRequest request) {
        return ApiResponse.success(newsService.listNews(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshNewsResponse> refreshNews(@Valid @RequestBody RefreshNewsRequest request) {
        return ApiResponse.success(newsService.refreshNews(request));
    }
}
