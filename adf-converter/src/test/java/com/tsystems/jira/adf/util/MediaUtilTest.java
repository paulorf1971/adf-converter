package com.tsystems.jira.adf.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tsystems.jira.adf.model.Media;

class MediaUtilTest {

    @Test
    void buildMediaUrl_appends_collection_and_slash() {
        Media media = new Media("123", "file", "coll", null, null);

        String url = MediaUtil.buildMediaUrl("https://cdn.example/media", media);

        assertThat(url).isEqualTo("https://cdn.example/media/123?collection=coll");
    }

    @Test
    void placeholder_prefers_base_url_over_media_prefix() {
        String placeholder = MediaUtil.placeholder("id-1", "file", "coll", "https://cdn.example");

        assertThat(placeholder).isEqualTo("https://cdn.example/id-1");
    }

    @Test
    void placeholder_without_base_uses_media_scheme_with_collection() {
        String placeholder = MediaUtil.placeholder("id-1", "file", "coll", "");

        assertThat(placeholder).isEqualTo("media:id-1?collection=coll");
    }

    @Test
    void fromImageSrc_strips_media_prefix_and_sets_type_file() {
        Media media = MediaUtil.fromImageSrc("media:abc");

        assertThat(media.getAttrs().get("id")).isEqualTo("abc");
        assertThat(media.getAttrs().get("type")).isEqualTo("file");
    }

    @Test
    void fromImageSrc_parses_collection_query() {
        Media media = MediaUtil.fromImageSrc("media:abc?collection=coll&type=file");

        assertThat(media.getAttrs().get("id")).isEqualTo("abc");
        assertThat(media.getAttrs().get("collection")).isEqualTo("coll");
        assertThat(media.getAttrs().get("type")).isEqualTo("file");
    }
}
