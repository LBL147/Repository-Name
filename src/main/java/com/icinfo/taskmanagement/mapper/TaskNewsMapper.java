package com.icinfo.taskmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icinfo.taskmanagement.dto.TaskNewsResponse;
import com.icinfo.taskmanagement.entity.TaskNews;
import java.util.List;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TaskNewsMapper extends BaseMapper<TaskNews> {

    @Select("""
            SELECT
                tn.id AS id,
                ni.id AS news_id,
                ni.title AS title,
                ni.url AS url,
                ni.source AS source,
                ni.keyword AS keyword,
                ni.published_at AS published_at,
                ni.fetched_at AS fetched_at,
                tn.created_at AS associated_at
            FROM task_news tn
            INNER JOIN news_items ni ON ni.id = tn.news_id
            WHERE tn.task_id = #{taskId}
            ORDER BY ni.published_at DESC, ni.fetched_at DESC, ni.id DESC
            """)
    @Results(id = "taskNewsResponseMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "news_id", property = "newsId"),
            @Result(column = "title", property = "title"),
            @Result(column = "url", property = "url"),
            @Result(column = "source", property = "source"),
            @Result(column = "keyword", property = "keyword"),
            @Result(column = "published_at", property = "publishedAt"),
            @Result(column = "fetched_at", property = "fetchedAt"),
            @Result(column = "associated_at", property = "associatedAt")
    })
    List<TaskNewsResponse> selectNewsByTaskId(@Param("taskId") Long taskId);
}
