package com.icinfo.taskmanagement.service.news;

import java.io.IOException;
import java.io.StringReader;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Component
public class RssNewsItemParser {

    public List<ExternalNewsItem> parse(String body, String defaultSource) {
        try {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
            NodeList nodes = document.getElementsByTagName("item");
            List<ExternalNewsItem> items = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (!(node instanceof Element element)) {
                    continue;
                }
                String title = text(element, "title");
                String url = text(element, "link");
                if (isBlank(title) || isBlank(url)) {
                    continue;
                }
                String description = text(element, "description");
                items.add(new ExternalNewsItem(
                        title,
                        url,
                        firstNonBlank(text(element, "source"), defaultSource),
                        parsePublishedAt(text(element, "pubDate")),
                        description));
            }
            return items;
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new NewsFetchException("RSS response is not valid XML", exception);
        }
    }

    private DocumentBuilderFactory secureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private LocalDateTime parsePublishedAt(String value) {
        if (isBlank(value)) {
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
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "RSS";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
