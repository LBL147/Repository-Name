package com.icinfo.taskmanagement.service.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GdeltNewsSource implements ExternalNewsSource {

    private static final String SOURCE_NAME = "GDELT DOC API";

    private static final DateTimeFormatter GDELT_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public GdeltNewsSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public NewsFetchResult fetch(String keyword) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        URI uri = URI.create("https://api.gdeltproject.org/api/v2/doc/doc"
                + "?query=" + encodedKeyword
                + "&mode=artlist"
                + "&format=json"
                + "&maxrecords=20"
                + "&sort=hybridrel");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new NewsFetchException("GDELT request failed with status " + response.statusCode());
            }
            return new NewsFetchResult(SOURCE_NAME, parseItems(response.body()));
        } catch (IOException exception) {
            throw new NewsFetchException("GDELT request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NewsFetchException("GDELT request interrupted", exception);
        }
    }

    private List<ExternalNewsItem> parseItems(String body) {
        try {
            JsonNode articles = objectMapper.readTree(body).path("articles");
            if (!articles.isArray()) {
                return List.of();
            }
            List<ExternalNewsItem> items = new ArrayList<>();
            for (JsonNode article : articles) {
                String title = text(article, "title");
                String url = text(article, "url");
                if (title == null || url == null) {
                    continue;
                }
                String source = firstNonBlank(
                        text(article, "sourceCommonName"),
                        text(article, "domain"),
                        SOURCE_NAME);
                items.add(new ExternalNewsItem(
                        title,
                        url,
                        source,
                        parsePublishedAt(text(article, "seendate"))));
            }
            return items;
        } catch (IOException exception) {
            throw new NewsFetchException("GDELT response is not valid JSON", exception);
        }
    }

    private LocalDateTime parsePublishedAt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, GDELT_DATE);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.ofInstant(
                        OffsetDateTime.parse(value).toInstant(),
                        ZoneId.systemDefault());
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return SOURCE_NAME;
    }
}
