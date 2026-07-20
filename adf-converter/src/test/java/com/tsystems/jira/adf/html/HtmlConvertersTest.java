package com.tsystems.jira.adf.html;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tsystems.jira.adf.api.ConverterContext;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.model.Blockquote;
import com.tsystems.jira.adf.model.BulletList;
import com.tsystems.jira.adf.model.CodeBlock;
import com.tsystems.jira.adf.model.DateNode;
import com.tsystems.jira.adf.model.DecisionItem;
import com.tsystems.jira.adf.model.DecisionList;
import com.tsystems.jira.adf.model.Document;
import com.tsystems.jira.adf.model.Emoji;
import com.tsystems.jira.adf.model.HardBreak;
import com.tsystems.jira.adf.model.Heading;
import com.tsystems.jira.adf.model.ListItem;
import com.tsystems.jira.adf.model.Media;
import com.tsystems.jira.adf.model.MediaSingle;
import com.tsystems.jira.adf.model.Panel;
import com.tsystems.jira.adf.model.Paragraph;
import com.tsystems.jira.adf.model.Status;
import com.tsystems.jira.adf.model.Table;
import com.tsystems.jira.adf.model.TableCell;
import com.tsystems.jira.adf.model.TableHeader;
import com.tsystems.jira.adf.model.TableRow;
import com.tsystems.jira.adf.model.TaskItem;
import com.tsystems.jira.adf.model.TaskList;
import com.tsystems.jira.adf.model.Text;
import com.tsystems.jira.adf.util.JsonUtil;

class HtmlConvertersTest {

    private final ConverterConfig config = ConverterConfig.builder().build();
    private final ConverterContext ctx = ConverterContext.builder().build();

    @Test
    void outbound_paragraph_marks_link() {
        Document doc = new Document(List.of(new Paragraph(List.of(
                new Text("Hello "),
                new Text("bold", List.of(com.tsystems.jira.adf.model.Mark.strong())),
                new Text(" link", List.of(com.tsystems.jira.adf.model.Mark.link("https://example.com", "title")))
        ))));

        String html = HtmlConverters.outbound(config).convert(doc, ctx);

        assertThat(html).contains("<p>");
        assertThat(html).contains("<strong>bold</strong>");
        assertThat(html).contains("<a href=\"https://example.com\" title=\"title\"> link</a>");
    }

    @Test
    void outbound_heading_blockquote_list_code_table_task_status_panel_media() {
        Document doc = new Document(List.of(
                new Heading(2, List.of(new Text("Title"))),
                new Blockquote(List.of(new Paragraph(List.of(new Text("quote"))))),
                new BulletList(List.of(new ListItem(List.of(new Paragraph(List.of(new Text("item1"))))))),
                new CodeBlock("java", "System.out.println(\"hi\");"),
                new Table(List.of(
                        new TableRow(List.of(
                                new TableHeader(List.of(new Text("H1")), null),
                                new TableHeader(List.of(new Text("H2")), null)
                        )),
                        new TableRow(List.of(
                                new TableCell(List.of(new Text("c1")), 2, 1, "#eee", "center", List.of()),
                                new TableCell(List.of(new Text("c2")), null)
                        ))
                )),
                new TaskList(List.of(new TaskItem("1", "DONE", List.of(new Paragraph(List.of(new Text("task"))))))),
                new Status("status text", "green", null),
                new Panel("info", List.of(new Paragraph(List.of(new Text("panel"))))),
                new MediaSingle(List.of(new Media("file123", "file", null, null, null)))
        ));

        String html = HtmlConverters.outbound(config).convert(doc, ctx);

        assertThat(html).contains("<h2>Title</h2>");
        assertThat(html).contains("<blockquote>");
        assertThat(html).contains("<ul>");
        assertThat(html).contains("<code class=\"language-java\">");
        assertThat(html).contains("<table>");
        assertThat(html).contains("colspan=\"2\"");
        assertThat(html).contains("rowspan=\"1\"");
        assertThat(html).contains("background:#eee;");
        assertThat(html).contains("class=\"task-list\"");
        assertThat(html).contains("data-status-color");
        assertThat(html).contains("data-panel-type=\"info\"");
        assertThat(html).contains("<img");
    }

    @Test
    void outbound_inline_nodes() {
        Document doc = new Document(List.of(new Paragraph(List.of(
                new Emoji(":smile:", null, "😊"),
                new HardBreak(),
                new DateNode("1700000000000")
        ))));

        String html = HtmlConverters.outbound(config).convert(doc, ctx);

        assertThat(html).contains("data-emoji-short-name=\":smile:\"");
        assertThat(html).contains("<br");
        assertThat(html).contains("<time");
    }

    @Test
    void inbound_basic_mapping() {
        String html = "<h2>Title</h2><p>Hello <strong>bold</strong> <a href='https://example.com'>link</a></p>";

        Document doc = HtmlConverters.inbound(config).convert(html, ctx);
        String json = JsonUtil.toJson(doc);

        assertThat(json).contains("\"type\":\"doc\"");
        assertThat(json).contains("\"heading\"");
        assertThat(json).contains("\"strong\"");
        assertThat(json).contains("\"link\"");
    }

    @Test
    void inbound_lists_tables_tasks_status_panel_media() {
        String html = "<ul class='task-list'><li><input type='checkbox' checked='checked'/>Task</li></ul>" +
                "<ul class='decision-list'><li id='d1'>Decision</li></ul>" +
                "<table><thead><tr><th>H1</th></tr></thead><tbody><tr><td colspan='2' align='center' style='background:#eee;'>c1</td></tr></tbody></table>" +
                "<div data-panel-type='info'><p>panel</p></div>" +
                "<span data-status-color='blue' data-status-text='stat'></span>" +
                "<img src='media123'/>";

        Document doc = HtmlConverters.inbound(config).convert(html, ctx);
        String json = JsonUtil.toJson(doc);

        assertThat(json).contains("taskList");
        assertThat(json).contains("decisionList");
        assertThat(json).contains("table");
        assertThat(json).contains("panel");
        assertThat(json).contains("status");
        assertThat(json).contains("media");
        assertThat(json).contains("colspan");
        assertThat(json).contains("rowspan");
    }

    @Test
    void outbound_media_placeholder_respects_config() {
        ConverterConfig cfg = ConverterConfig.builder().mediaBaseUrl("https://cdn.example/").mediaPlaceholder(true).build();
        Document doc = new Document(List.of(new MediaSingle(List.of(new Media("abc", "file", "coll", null, null)))));

        String html = HtmlConverters.outbound(cfg).convert(doc, ctx);

        assertThat(html).contains("https://cdn.example/abc");
    }

    @Test
    void roundtrip_html_to_markdown_preserves_table_and_media() {
        String html = "<h1>Title</h1><p>para</p><table><tr><th>H</th></tr><tr><td>c1</td></tr></table><img src='media:123'/>";

        Document adf = HtmlConverters.inbound(config).convert(html, ctx);
        String md = com.tsystems.jira.adf.markdown.MarkdownConverters.outbound(config).convert(adf, ctx);

        assertThat(md).contains("# Title");
        assertThat(md).contains("| H |");
        assertThat(md).contains("media:123");
    }

    @Test
    void round_trip_sanity() {
        Document doc = new Document(List.of(
                new Paragraph(List.of(new Text("Hello"))),
                new Heading(1, List.of(new Text("H"))),
                new TaskList(List.of(new TaskItem("1", "TODO", List.of(new Paragraph(List.of(new Text("task")))))))
        ));

        String html = HtmlConverters.outbound(config).convert(doc, ctx);
        Document back = HtmlConverters.inbound(config).convert(html, ctx);

        String json = JsonUtil.toJson(back);
        assertThat(json).contains("\"taskList\"");
        assertThat(json).contains("\"heading\"");
        assertThat(json).contains("\"paragraph\"");
    }
}
