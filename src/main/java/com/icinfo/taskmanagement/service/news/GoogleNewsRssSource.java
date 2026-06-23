package com.icinfo.taskmanagement.service.news;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Component
public class GoogleNewsRssSource implements ExternalNewsSource {

    private static final String SOURCE_NAME = "Google News RSS";

    private final HttpClient httpClient;

    public GoogleNewsRssSource() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public NewsFetchResult fetch(String keyword) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        URI uri = URI.create("https://news.google.com/rss/search"
                + "?q=" + encodedKeyword
                + "&hl=zh-CN"
                + "&gl=CN"
                + "&ceid=CN:zh-Hans");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new NewsFetchException("RSS request failed with status " + response.statusCode());
            }
            return new NewsFetchResult(SOURCE_NAME, parseItems(response.body()));
        } catch (IOException exception) {
            throw new NewsFetchException("RSS request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NewsFetchException("RSS request interrupted", exception);
        }
    }

    private List<ExternalNewsItem> parseItems(String body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
            NodeList nodes = document.getElementsByTagName("item");
            List<ExternalNewsItem> items = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                String title = text(element, "title");
                String url = text(element, "link");
                if (title == null || url == null) {
                    continue;
                }
                items.add(new ExternalNewsItem(
                        title,
                        url,
                        firstNonBlank(text(element, "source"), SOURCE_NAME),
                        parsePublishedAt(text(element, "pubDate"))));
            }
            return items;
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new NewsFetchException("RSS response is not valid XML", exception);
        }
    }

    private LocalDateTime parsePublishedAt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(
                    ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(),
                    ZoneId.systemDefault());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return null;
        }
        String text = nodes.item(0).getTextContent().trim();
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
