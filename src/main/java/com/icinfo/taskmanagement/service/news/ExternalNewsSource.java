package com.icinfo.taskmanagement.service.news;

import java.util.List;

public interface ExternalNewsSource {

    NewsFetchResult fetch(String keyword);
}
