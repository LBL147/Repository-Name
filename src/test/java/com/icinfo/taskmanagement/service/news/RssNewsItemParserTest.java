package com.icinfo.taskmanagement.service.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RssNewsItemParserTest {

    private final RssNewsItemParser parser = new RssNewsItemParser();

    @Test
    void parsesRssItemFields() {
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <item>
                            <title>Java 生态更新</title>
                            <link>https://example.com/java-news</link>
                            <source>Example Source</source>
                            <pubDate>Tue, 25 Jun 2024 10:15:30 GMT</pubDate>
                            <description>Spring Boot 和 AI 工具链动态</description>
                        </item>
                    </channel>
                </rss>
                """;

        List<ExternalNewsItem> items = parser.parse(rss, "Default Source");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getTitle()).isEqualTo("Java 生态更新");
        assertThat(items.get(0).getUrl()).isEqualTo("https://example.com/java-news");
        assertThat(items.get(0).getSource()).isEqualTo("Example Source");
        assertThat(items.get(0).getPublishedAt()).isNotNull();
        assertThat(items.get(0).getDescription()).isEqualTo("Spring Boot 和 AI 工具链动态");
    }

    @Test
    void usesDefaultSourceWhenItemSourceIsMissing() {
        String rss = """
                <rss version="2.0">
                    <channel>
                        <item>
                            <title>AI 新闻</title>
                            <link>https://example.com/ai-news</link>
                        </item>
                    </channel>
                </rss>
                """;

        List<ExternalNewsItem> items = parser.parse(rss, "InfoQ");

        assertThat(items).singleElement()
                .extracting(ExternalNewsItem::getSource)
                .isEqualTo("InfoQ");
    }
}
