package com.icinfo.taskmanagement.service;

import com.icinfo.taskmanagement.common.ErrorCode;
import com.icinfo.taskmanagement.dto.NewsItemResponse;
import com.icinfo.taskmanagement.dto.RefreshNewsRequest;
import com.icinfo.taskmanagement.dto.RefreshNewsResponse;
import com.icinfo.taskmanagement.dto.RefreshTaskNewsRequest;
import com.icinfo.taskmanagement.dto.RefreshTaskNewsResponse;
import com.icinfo.taskmanagement.dto.TaskNewsResponse;
import com.icinfo.taskmanagement.entity.Task;
import com.icinfo.taskmanagement.entity.TaskNews;
import com.icinfo.taskmanagement.exception.BusinessException;
import com.icinfo.taskmanagement.mapper.TaskNewsMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class TaskNewsService {

    private final TaskService taskService;

    private final NewsService newsService;

    private final TaskNewsMapper taskNewsMapper;

    public TaskNewsService(TaskService taskService, NewsService newsService, TaskNewsMapper taskNewsMapper) {
        this.taskService = taskService;
        this.newsService = newsService;
        this.taskNewsMapper = taskNewsMapper;
    }

    public List<TaskNewsResponse> listTaskNews(Long taskId) {
        taskService.getVisibleTaskEntity(taskId);
        return taskNewsMapper.selectNewsByTaskId(taskId);
    }

    public RefreshTaskNewsResponse refreshTaskNews(Long taskId, RefreshTaskNewsRequest request) {
        Task task = taskService.getVisibleTaskEntity(taskId);
        String keyword = resolveKeyword(request, task);

        RefreshNewsResponse newsResponse;
        try {
            RefreshNewsRequest refreshNewsRequest = new RefreshNewsRequest();
            refreshNewsRequest.setKeyword(keyword);
            newsResponse = newsService.refreshNews(refreshNewsRequest);
        } catch (BusinessException exception) {
            if (exception.getErrorCode() != ErrorCode.BUSINESS_ERROR) {
                throw exception;
            }
            return new RefreshTaskNewsResponse(
                    keyword,
                    taskNewsMapper.selectNewsByTaskId(taskId),
                    0,
                    0,
                    0,
                    null,
                    false,
                    false,
                    exception.getMessage());
        }

        int associatedCount = associateNews(taskId, newsResponse.getRecords());
        return new RefreshTaskNewsResponse(
                keyword,
                taskNewsMapper.selectNewsByTaskId(taskId),
                newsResponse.getFetchedCount(),
                newsResponse.getInsertedCount(),
                associatedCount,
                newsResponse.getSource(),
                newsResponse.isCacheFallback(),
                true,
                newsResponse.getMessage());
    }

    private int associateNews(Long taskId, List<NewsItemResponse> newsItems) {
        LocalDateTime now = LocalDateTime.now();
        int associatedCount = 0;
        for (NewsItemResponse newsItem : newsItems) {
            TaskNews taskNews = new TaskNews();
            taskNews.setTaskId(taskId);
            taskNews.setNewsId(newsItem.getId());
            taskNews.setCreatedAt(now);
            try {
                taskNewsMapper.insert(taskNews);
                associatedCount++;
            } catch (DuplicateKeyException ignored) {
                // task_id + news_id is unique; repeated refreshes should be idempotent.
            }
        }
        return associatedCount;
    }

    private String resolveKeyword(RefreshTaskNewsRequest request, Task task) {
        String manualKeyword = request == null ? null : normalizeKeyword(request.getKeyword());
        if (manualKeyword != null) {
            return manualKeyword;
        }
        String taskTitle = normalizeKeyword(task.getTitle());
        if (taskTitle == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务标题不能为空");
        }
        return taskTitle;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
