package com.icinfo.taskmanagement.service.news;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DomesticRssNewsSource implements ExternalNewsSource {

    private static final String SOURCE_NAME = "Domestic RSS";

    private static final Pattern CHARSET_PATTERN =
            Pattern.compile("charset=[\"']?([^;\"'\\s>]+)", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;

    private final RssNewsItemParser parser;

    private final List<RssFeed> feeds;

    @Autowired
    public DomesticRssNewsSource(RssNewsItemParser parser) {
        this(parser, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), defaultFeeds());
    }

    DomesticRssNewsSource(RssNewsItemParser parser, HttpClient httpClient, List<RssFeed> feeds) {
        this.parser = parser;
        this.httpClient = httpClient;
        this.feeds = feeds;
    }

    @Override
    public NewsFetchResult fetch(String keyword) {
        List<ExternalNewsItem> items = new ArrayList<>();
        int failedCount = 0;
        for (RssFeed feed : feeds) {
            try {
                String body = fetchFeed(feed.uri());
                items.addAll(parser.parse(body, feed.source()));
            } catch (NewsFetchException exception) {
                failedCount++;
            }
        }
        if (failedCount == feeds.size()) {
            throw new NewsFetchException("Domestic RSS sources are unavailable");
        }
        return new NewsFetchResult(SOURCE_NAME, filterDeduplicateAndSort(items, keyword));
    }

    private String fetchFeed(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/126.0.0.0 Safari/537.36")
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new NewsFetchException("RSS request failed with status " + response.statusCode());
            }
            return decode(response.body(), response.headers());
        } catch (IOException exception) {
            throw new NewsFetchException("RSS request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NewsFetchException("RSS request interrupted", exception);
        }
    }

    private String decode(byte[] body, HttpHeaders headers) {
        Charset headerCharset = headers.firstValue("content-type")
                .flatMap(this::charsetFromText)
                .orElse(null);
        if (headerCharset != null) {
            return new String(body, headerCharset);
        }
        String utf8Text = new String(body, StandardCharsets.UTF_8);
        Optional<Charset> xmlCharset = charsetFromText(utf8Text);
        if (xmlCharset.isPresent() && !xmlCharset.get().equals(StandardCharsets.UTF_8)) {
            return new String(body, xmlCharset.get());
        }
        return utf8Text;
    }

    private Optional<Charset> charsetFromText(String value) {
        Matcher matcher = CHARSET_PATTERN.matcher(value);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Charset.forName(matcher.group(1)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private List<ExternalNewsItem> filterDeduplicateAndSort(List<ExternalNewsItem> items, String keyword) {
        Map<String, ExternalNewsItem> uniqueItems = new LinkedHashMap<>();
        for (ExternalNewsItem item : items) {
            if (isBlank(item.getTitle()) || isBlank(item.getUrl()) || !matchesKeyword(item, keyword)) {
                continue;
            }
            uniqueItems.putIfAbsent(item.getUrl().trim(), item);
        }
        return uniqueItems.values().stream()
                .sorted(this::comparePublishedAtDesc)
                .toList();
    }

    private int comparePublishedAtDesc(ExternalNewsItem left, ExternalNewsItem right) {
        LocalDateTime leftPublishedAt = left.getPublishedAt();
        LocalDateTime rightPublishedAt = right.getPublishedAt();
        if (leftPublishedAt == null && rightPublishedAt == null) {
            return 0;
        }
        if (leftPublishedAt == null) {
            return 1;
        }
        if (rightPublishedAt == null) {
            return -1;
        }
        return rightPublishedAt.compareTo(leftPublishedAt);
    }

    private boolean matchesKeyword(ExternalNewsItem item, String keyword) {
        List<String> tokens = keywordTokens(keyword);
        if (tokens.isEmpty()) {
            return true;
        }
        String haystack = (firstNonBlank(item.getTitle(), "") + " "
                + firstNonBlank(item.getDescription(), "") + " "
                + firstNonBlank(item.getSource(), "")).toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (haystack.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private List<String> keywordTokens(String keyword) {
        if (isBlank(keyword)) {
            return List.of();
        }
        return Pattern.compile("[\\s,，;；、]+")
                .splitAsStream(keyword.trim().toLowerCase(Locale.ROOT))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static List<RssFeed> defaultFeeds() {
        return List.of(
                new RssFeed("OSCHINA", URI.create("https://www.oschina.net/news/rss")),
                new RssFeed("InfoQ", URI.create("https://www.infoq.cn/feed")),
                new RssFeed("36氪", URI.create("https://36kr.com/feed")),
                new RssFeed("博客园新闻", URI.create("https://www.cnblogs.com/news/rss")));
    }

    record RssFeed(String source, URI uri) {
    }
}
