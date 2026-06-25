package com.icinfo.taskmanagement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icinfo.taskmanagement.common.ErrorCode;
import com.icinfo.taskmanagement.common.PageResponse;
import com.icinfo.taskmanagement.dto.NewsItemResponse;
import com.icinfo.taskmanagement.dto.NewsQueryRequest;
import com.icinfo.taskmanagement.dto.RefreshNewsRequest;
import com.icinfo.taskmanagement.dto.RefreshNewsResponse;
import com.icinfo.taskmanagement.entity.NewsItem;
import com.icinfo.taskmanagement.exception.BusinessException;
import com.icinfo.taskmanagement.mapper.NewsItemMapper;
import com.icinfo.taskmanagement.service.news.ExternalNewsItem;
import com.icinfo.taskmanagement.service.news.NewsFetchException;
import com.icinfo.taskmanagement.service.news.NewsFetchResult;
import com.icinfo.taskmanagement.service.news.NewsFetcher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsService {

    private static final int REFRESH_RESPONSE_SIZE = 20;

    private final NewsItemMapper newsItemMapper;

    private final NewsFetcher newsFetcher;

    public NewsService(NewsItemMapper newsItemMapper, NewsFetcher newsFetcher) {
        this.newsItemMapper = newsItemMapper;
        this.newsFetcher = newsFetcher;
    }

    public PageResponse<NewsItemResponse> listNews(NewsQueryRequest request) {
        String keyword = normalizeKeyword(request.getKeyword());
        long pageNumber = normalizePage(request.getPage());
        long pageSize = normalizeSize(request.getSize());
        IPage<NewsItem> newsPage = newsItemMapper.selectPage(
                new Page<>(pageNumber, pageSize),
                buildNewsQuery(keyword));
        List<NewsItemResponse> records = newsPage.getRecords().stream()
                .map(NewsItemResponse::from)
                .toList();
        return new PageResponse<>(records, newsPage.getTotal(), pageNumber, pageSize);
    }

    @Transactional
    public RefreshNewsResponse refreshNews(RefreshNewsRequest request) {
        String keyword = normalizeRequiredKeyword(request.getKeyword());
        NewsFetchResult fetchResult;
        try {
            fetchResult = newsFetcher.fetch(keyword);
        } catch (NewsFetchException exception) {
            PageResponse<NewsItemResponse> cached = cachedPage(keyword, REFRESH_RESPONSE_SIZE);
            if (!cached.getRecords().isEmpty()) {
                return new RefreshNewsResponse(
                        cached.getRecords(),
                        0,
                        0,
                        null,
                        true,
                        "外部资讯暂时不可用，已返回缓存数据");
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "外部资讯暂时不可用，请稍后重试");
        }

        int insertedCount = cacheFetchedItems(keyword, fetchResult.getItems());
        PageResponse<NewsItemResponse> latestCached = cachedPage(keyword, REFRESH_RESPONSE_SIZE);
        return new RefreshNewsResponse(
                latestCached.getRecords(),
                fetchResult.getItems().size(),
                insertedCount,
                fetchResult.getSource(),
                false,
                "刷新成功");
    }

    private int cacheFetchedItems(String keyword, List<ExternalNewsItem> fetchedItems) {
        LocalDateTime fetchedAt = LocalDateTime.now();
        Map<String, ExternalNewsItem> uniqueItems = new LinkedHashMap<>();
        for (ExternalNewsItem item : fetchedItems) {
            if (isBlank(item.getTitle()) || isBlank(item.getUrl())) {
                continue;
            }
            uniqueItems.putIfAbsent(item.getUrl().trim(), item);
        }

        int insertedCount = 0;
        for (Map.Entry<String, ExternalNewsItem> entry : uniqueItems.entrySet()) {
            String url = entry.getKey();
            String urlHash = sha256(url);
            if (existsByUrlHash(urlHash)) {
                continue;
            }
            ExternalNewsItem externalItem = entry.getValue();
            NewsItem newsItem = new NewsItem();
            newsItem.setTitle(limit(externalItem.getTitle().trim(), 512));
            newsItem.setUrl(url);
            newsItem.setUrlHash(urlHash);
            newsItem.setSource(limit(normalizeSource(externalItem.getSource()), 128));
            newsItem.setKeyword(keyword);
            newsItem.setPublishedAt(externalItem.getPublishedAt());
            newsItem.setFetchedAt(fetchedAt);
            try {
                newsItemMapper.insert(newsItem);
                insertedCount++;
            } catch (DuplicateKeyException ignored) {
                // Another refresh may have cached the same URL; the unique index keeps storage idempotent.
            }
        }
        return insertedCount;
    }

    private PageResponse<NewsItemResponse> cachedPage(String keyword, long size) {
        NewsQueryRequest request = new NewsQueryRequest();
        request.setKeyword(keyword);
        request.setPage(1L);
        request.setSize(size);
        return listNews(request);
    }

    private LambdaQueryWrapper<NewsItem> buildNewsQuery(String keyword) {
        LambdaQueryWrapper<NewsItem> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null) {
            wrapper.and(keywordWrapper -> keywordWrapper
                    .like(NewsItem::getKeyword, keyword)
                    .or()
                    .like(NewsItem::getTitle, keyword));
        }
        return wrapper
                .orderByDesc(NewsItem::getPublishedAt)
                .orderByDesc(NewsItem::getFetchedAt)
                .orderByDesc(NewsItem::getId);
    }

    private boolean existsByUrlHash(String urlHash) {
        Long count = newsItemMapper.selectCount(new LambdaQueryWrapper<NewsItem>()
                .eq(NewsItem::getUrlHash, urlHash));
        return count != null && count > 0;
    }

    private String normalizeRequiredKeyword(String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "关键词不能为空");
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long normalizePage(Long page) {
        if (page == null || page < 1) {
            return 1L;
        }
        return page;
    }

    private long normalizeSize(Long size) {
        if (size == null || size < 1) {
            return 10L;
        }
        return Math.min(size, 100L);
    }

    private String normalizeSource(String source) {
        if (isBlank(source)) {
            return "未知来源";
        }
        return source.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String limit(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
