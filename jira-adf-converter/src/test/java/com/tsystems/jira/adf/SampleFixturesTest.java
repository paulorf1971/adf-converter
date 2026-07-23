package com.tsystems.jira.adf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.tsystems.jira.adf.api.ConverterContext;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.html.HtmlConverters;
import com.tsystems.jira.adf.markdown.MarkdownConverters;
import com.tsystems.jira.adf.model.Document;
import com.tsystems.jira.adf.model.Media;
import com.tsystems.jira.adf.model.MediaSingle;
import com.tsystems.jira.adf.plaintext.PlainTextConverters;
import com.tsystems.jira.adf.util.JsonUtil;

class SampleFixturesTest {

    private final ConverterConfig config = ConverterConfig.builder().build();
    private final ConverterContext ctx = ConverterContext.builder().build();

    @Test
    void sample_adf_json_deserializes_and_renders_all_formats() {
        Document doc = JsonUtil.fromJson(resource("samples/sample.adf.json"), Document.class);

        assertThat(MarkdownConverters.outbound(config).convert(doc, ctx)).contains("# Sample", "media:file-1?collection=coll");
        assertThat(HtmlConverters.outbound(config).convert(doc, ctx)).contains("<h1>Sample</h1>", "data-media-collection=\"coll\"");
        assertThat(PlainTextConverters.outbound(config).convert(doc, ctx)).contains("Sample", "[media:file-1?collection=coll]");
    }

    @Test
    void sample_markdown_html_and_plaintext_parse_expected_structures() {
        Document markdown = MarkdownConverters.inbound(config).convert(resource("samples/sample.md"), ctx);
        Document html = HtmlConverters.inbound(config).convert(resource("samples/sample.html"), ctx);
        Document text = PlainTextConverters.inbound(config).convert(resource("samples/sample.txt"), ctx);

        assertThat(JsonUtil.toJson(markdown)).contains("heading", "taskList", "table", "media");
        assertThat(JsonUtil.toJson(html)).contains("heading", "taskList", "table", "panel", "status", "date", "media");
        assertThat(JsonUtil.toJson(text)).contains("orderedList", "taskList", "table", "media");
        Media media = (Media) ((MediaSingle) text.getContent().get(text.getContent().size() - 1)).getContent().get(0);
        assertThat(media.getAttrs().get("collection")).isEqualTo("coll");
    }

    private String resource(String path) {
        try (var in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test resource: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
