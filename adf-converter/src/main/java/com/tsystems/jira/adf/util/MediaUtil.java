package com.tsystems.jira.adf.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import com.tsystems.jira.adf.model.Media;

/**
 * Utility helpers for media URL generation and placeholders.
 */
public final class MediaUtil {

    private MediaUtil() {
    }

    public static String buildMediaUrl(String baseUrl, Media media) {
        if (media == null) {
            return "";
        }
        Map<String, Object> attrs = media.getAttrs();
        String id = attrs == null ? "" : Objects.toString(attrs.getOrDefault("id", ""));
        String collection = attrs == null ? null : (String) attrs.get("collection");
        String normalizedBase = baseUrl == null ? "" : baseUrl;
        if (!normalizedBase.isEmpty() && !normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase + "/";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(normalizedBase).append(id);
        if (collection != null && !collection.isBlank()) {
            sb.append("?collection=").append(URLEncoder.encode(collection, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    public static String placeholder(String id, String type, String collection, String fallbackFromConfig) {
        String base = fallbackFromConfig == null ? "" : fallbackFromConfig;
        if (!base.isBlank()) {
            if (!base.endsWith("/")) {
                base = base + "/";
            }
            return base + Objects.toString(id, "");
        }
        String suffix = collection == null || collection.isBlank() ? Objects.toString(id, "")
                : Objects.toString(id, "") + "?collection=" + collection;
        return "media:" + suffix;
    }

    public static Media fromImageSrc(String src) {
        String id = src == null ? "" : src;
        if (id.startsWith("media:")) {
            id = id.substring("media:".length());
        }
        return new Media(id, "file", null, null, null);
    }
}

