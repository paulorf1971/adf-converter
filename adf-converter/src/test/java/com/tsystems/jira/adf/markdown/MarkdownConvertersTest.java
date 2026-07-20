package com.tsystems.jira.adf.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tsystems.jira.adf.api.ConverterContext;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.model.Blockquote;
import com.tsystems.jira.adf.model.BulletList;
import com.tsystems.jira.adf.model.CodeBlock;
import com.tsystems.jira.adf.model.Document;
import com.tsystems.jira.adf.model.Heading;
import com.tsystems.jira.adf.model.ListItem;
import com.tsystems.jira.adf.model.Media;
import com.tsystems.jira.adf.model.MediaSingle;
import com.tsystems.jira.adf.model.Paragraph;
import com.tsystems.jira.adf.model.Table;
import com.tsystems.jira.adf.model.TableCell;
import com.tsystems.jira.adf.model.TableHeader;
import com.tsystems.jira.adf.model.TableRow;
import com.tsystems.jira.adf.model.TaskItem;
import com.tsystems.jira.adf.model.TaskList;
import com.tsystems.jira.adf.model.Text;
import com.tsystems.jira.adf.util.JsonUtil;

class MarkdownConvertersTest {

    private final ConverterConfig config = ConverterConfig.builder().build();
    private final ConverterContext ctx = ConverterContext.builder().build();

    @Test
    void outbound_paragraph_and_marks() {
        Document doc = new Document(List.of(new Paragraph(List.of(
                new Text("Hello "),
                new Text("bold", List.of(com.tsystems.jira.adf.model.Mark.strong())),
                new Text(" world")
        ))));

        String md = MarkdownConverters.outbound(config).convert(doc, ctx);

        assertThat(md).isEqualTo("Hello **bold** world");
    }

    @Test
    void outbound_heading_blockquote_list_code_table_task() {
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
                                new TableCell(List.of(new Text("c1")), null),
                                new TableCell(List.of(new Text("c2")), null)
                        ))
                )),
                new TaskList(List.of(new TaskItem("1", "DONE", List.of(new Paragraph(List.of(new Text("task")))))))
        ));

        String md = MarkdownConverters.outbound(config).convert(doc, ctx);

        assertThat(md).contains("## Title");
        assertThat(md).contains("> quote");
        assertThat(md).contains("* item1");
        assertThat(md).contains("```java");
        assertThat(md).contains("| H1 |");
        assertThat(md).contains("- [x] task");
    }

    @Test
    void inbound_simple_roundtrip() {
        String markdown = "## Title\n\nParagraph text with **bold** and _em_.\n\n- [x] done\n- [ ] todo\n\n| H1 | H2 |\n| --- | --- |\n| c1 | c2 |\n![file](media:123)\n";

        Document doc = MarkdownConverters.inbound(config).convert(markdown, ctx);
        String json = JsonUtil.toJson(doc);

        assertThat(json).contains("\"type\":\"doc\"");
        String mdBack = MarkdownConverters.outbound(config).convert(doc, ctx);
        assertThat(mdBack).contains("## Title");
        assertThat(mdBack).contains("- [x] done");
        assertThat(mdBack).contains("| H1 |");
        assertThat(mdBack).contains("media:123");
    }

    @Test
    void outbound_media_placeholder_and_table_defaults() {
        Document doc = new Document(List.of(
                new Table(List.of(
                        new TableRow(List.of(new TableHeader(List.of(new Text("H1")), null))),
                        new TableRow(List.of(new TableCell(List.of(new Text("c1")), null)))
                )),
                new MediaSingle(List.of(new Media("id1", "file", "coll", null, null)))
        ));

        String md = MarkdownConverters.outbound(config).convert(doc, ctx);

        assertThat(md).contains("| H1 |");
        assertThat(md).contains("media:id1");
    }
}
