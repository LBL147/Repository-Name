package com.icinfo.taskmanagement.service.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DomesticRssNewsSourceTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void filtersDeduplicatesSortsAndIgnoresFailedFeed() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/failed", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.createContext("/rss", exchange -> {
            byte[] body = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <rss version="2.0">
                        <channel>
                            <item>
                                <title>AI 新工具发布</title>
                                <link>https://example.com/ai-new</link>
                                <pubDate>Tue, 25 Jun 2024 10:15:30 GMT</pubDate>
                                <description>面向 Java 团队</description>
                            </item>
                            <item>
                                <title>重复 AI 新工具发布</title>
                                <link>https://example.com/ai-new</link>
                                <pubDate>Tue, 25 Jun 2024 11:15:30 GMT</pubDate>
                                <description>重复 URL</description>
                            </item>
                            <item>
                                <title>Spring Boot 实践</title>
                                <link>https://example.com/spring</link>
                                <pubDate>Tue, 25 Jun 2024 12:15:30 GMT</pubDate>
                                <description>后端工程</description>
                            </item>
                        </channel>
                    </rss>
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/rss+xml; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        DomesticRssNewsSource source = newSource(List.of(
                new DomesticRssNewsSource.RssFeed("Broken", uri("/failed")),
                new DomesticRssNewsSource.RssFeed("Local RSS", uri("/rss"))));

        NewsFetchResult result = source.fetch("AI");

        assertThat(result.getSource()).isEqualTo("Domestic RSS");
        assertThat(result.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getTitle()).isEqualTo("AI 新工具发布");
                    assertThat(item.getSource()).isEqualTo("Local RSS");
                });
    }

    @Test
    void throwsOnlyWhenAllFeedsFail() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/failed", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        DomesticRssNewsSource source = newSource(List.of(
                new DomesticRssNewsSource.RssFeed("Broken", uri("/failed"))));

        assertThatThrownBy(() -> source.fetch("AI"))
                .isInstanceOf(NewsFetchException.class)
                .hasMessage("Domestic RSS sources are unavailable");
    }

    private DomesticRssNewsSource newSource(List<DomesticRssNewsSource.RssFeed> feeds) {
        return new DomesticRssNewsSource(
                new RssNewsItemParser(),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                feeds);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }
}
