package com.tsystems.jira.kong.client.model.text.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class MarkdownToAdfConverter {

    private MarkdownToAdfConverter() {}

    public static ObjectNode toAdf(String markdown, ObjectMapper mapper) {
        ObjectNode doc = mapper.createObjectNode();
        doc.put("type", "doc");
        doc.put("version", 1);
        ArrayNode content = mapper.createArrayNode();

        String[] lines = markdown.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith("h")) {
                content.add(headingNode(mapper, line));
            } else if (line.startsWith("* ")) {
                content.add(bulletItemNode(mapper, line.substring(2)));
            } else if (line.startsWith("# ")) {
                content.add(orderedItemNode(mapper, line.substring(2)));
            } else if (line.startsWith("bq. ")) {
                content.add(blockquoteNode(mapper, line.substring(4)));
            } else if (!line.isBlank()) {
                content.add(paragraphNode(mapper, line));
            }
        }

        doc.set("content", content);
        return doc;
    }

    private static ObjectNode paragraphNode(ObjectMapper m, String text) {
        ObjectNode p = m.createObjectNode();
        p.put("type", "paragraph");
        ArrayNode content = m.createArrayNode();
        content.add(textNode(m, text));
        p.set("content", content);
        return p;
    }

    private static ObjectNode headingNode(ObjectMapper m, String line) {
        int level = Character.getNumericValue(line.charAt(1));
        String text = line.substring(3);
        
        ObjectNode h = m.createObjectNode();
        h.put("type", "heading");
        ObjectNode attrs = m.createObjectNode();
        attrs.put("level", level);
        h.set("attrs", attrs);

        ArrayNode content = m.createArrayNode();
        content.add(textNode(m, text));
        h.set("content", content);
        return h;
    }

    private static ObjectNode bulletItemNode(ObjectMapper m, String text) {
        // Simplified single item list
        ObjectNode list = m.createObjectNode();
        list.put("type", "bulletList");
        ArrayNode items = m.createArrayNode();
        items.add(listItemNode(m, text));
        list.set("content", items);
        return list;
    }

    private static ObjectNode orderedItemNode(ObjectMapper m, String text) {
        ObjectNode list = m.createObjectNode();
        list.put("type", "orderedList");
        ArrayNode items = m.createArrayNode();
        items.add(listItemNode(m, text));
        list.set("content", items);
        return list;
    }

    private static ObjectNode listItemNode(ObjectMapper m, String text) {
        ObjectNode li = m.createObjectNode();
        li.put("type", "listItem");
        ArrayNode content = m.createArrayNode();
        content.add(paragraphNode(m, text));
        li.set("content", content);
        return li;
    }

    private static ObjectNode blockquoteNode(ObjectMapper m, String text) {
        ObjectNode bq = m.createObjectNode();
        bq.put("type", "blockquote");
        ArrayNode content = m.createArrayNode();
        content.add(paragraphNode(m, text));
        bq.set("content", content);
        return bq;
    }

    private static ObjectNode textNode(ObjectMapper m, String text) {
        ObjectNode t = m.createObjectNode();
        t.put("type", "text");
        t.put("text", text);
        return t;
    }
}
