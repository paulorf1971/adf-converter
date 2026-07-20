package com.tsystems.jira.adf.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.tsystems.jira.adf.api.ConverterContext;
import com.tsystems.jira.adf.api.ConversionException;
import com.tsystems.jira.adf.api.InboundConverter;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.model.AdfNode;
import com.tsystems.jira.adf.model.Blockquote;
import com.tsystems.jira.adf.model.BulletList;
import com.tsystems.jira.adf.model.CodeBlock;
import com.tsystems.jira.adf.model.DateNode;
import com.tsystems.jira.adf.model.Document;
import com.tsystems.jira.adf.model.Emoji;
import com.tsystems.jira.adf.model.HardBreak;
import com.tsystems.jira.adf.model.Heading;
import com.tsystems.jira.adf.model.ListItem;
import com.tsystems.jira.adf.model.Mark;
import com.tsystems.jira.adf.model.Media;
import com.tsystems.jira.adf.model.MediaInline;
import com.tsystems.jira.adf.model.OrderedList;
import com.tsystems.jira.adf.model.Paragraph;
import com.tsystems.jira.adf.model.Table;
import com.tsystems.jira.adf.model.TableCell;
import com.tsystems.jira.adf.model.TableHeader;
import com.tsystems.jira.adf.model.TableRow;
import com.tsystems.jira.adf.model.TaskItem;
import com.tsystems.jira.adf.model.TaskList;
import com.tsystems.jira.adf.model.Text;
import com.tsystems.jira.adf.util.MediaUtil;
import com.tsystems.jira.adf.util.TableUtil;

import com.vladsch.flexmark.ast.AutoLink;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * Markdown → ADF converter using flexmark AST walk.
 */
public class MarkdownToAdfConverter implements InboundConverter<String> {

    private final ConverterConfig config;
    private final Parser parser;

    public MarkdownToAdfConverter(ConverterConfig config) {
        this.config = Optional.ofNullable(config).orElse(ConverterConfig.builder().build());
        this.parser = createParser(this.config);
    }

    @Override
    public Document convert(String input, ConverterContext context) throws ConversionException {
        if (input == null) {
            return new Document(new ArrayList<>());
        }
        com.vladsch.flexmark.util.ast.Document doc = parser.parse(input);
        List<AdfNode> content = new ArrayList<>();
        for (Node child = doc.getFirstChild(); child != null; child = child.getNext()) {
            AdfNode mapped = mapNode(child);
            if (mapped != null) {
                content.add(mapped);
            }
        }
        return new Document(1, content);
    }

    private Parser createParser(ConverterConfig config) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                TaskListExtension.create(),
                AutolinkExtension.create(),
                EmojiExtension.create(),
                StrikethroughExtension.create()
        ));
        return Parser.builder(options).build();
    }

    private AdfNode mapNode(Node node) {
        if (node instanceof com.vladsch.flexmark.ast.Paragraph p) {
            return new Paragraph(mapInlineChildren(p));
        }
        if (node instanceof com.vladsch.flexmark.ast.Heading h) {
            List<AdfNode> inline = mapInlineChildren(h);
            return new Heading(h.getLevel(), inline);
        }
        if (node instanceof BlockQuote bq) {
            List<AdfNode> blocks = new ArrayList<>();
            for (Node child = bq.getFirstChild(); child != null; child = child.getNext()) {
                AdfNode mapped = mapNode(child);
                if (mapped != null) {
                    blocks.add(mapped);
                }
            }
            return new Blockquote(blocks.isEmpty() ? List.of(new Paragraph(mapInlineChildren(bq))) : blocks);
        }
        if (node instanceof com.vladsch.flexmark.ast.OrderedList ol) {
            if (containsTaskItems(ol)) {
                return new TaskList(mapTaskItems(ol));
            }
            return new OrderedList(mapListChildren(ol));
        }
        if (node instanceof com.vladsch.flexmark.ast.BulletList bl) {
            if (containsTaskItems(bl)) {
                return new TaskList(mapTaskItems(bl));
            }
            return new BulletList(mapListChildren(bl));
        }
        if (node instanceof FencedCodeBlock cb) {
            String literal = cb.getContentChars().toString();
            String info = cb.getInfo().toString();
            String lang = info.isBlank() ? null : info;
            return new CodeBlock(lang, literal);
        }
        if (node instanceof TableBlock table) {
            return TableUtil.normalize(mapTable(table));
        }
        if (node instanceof HtmlBlock || node instanceof HtmlInline) {
            return null; // ignore unsafe HTML
        }
        if (node instanceof com.vladsch.flexmark.ast.Text || node instanceof StrongEmphasis || node instanceof Emphasis
                || node instanceof Code || node instanceof Link || node instanceof AutoLink
                || node instanceof Strikethrough || node instanceof SoftLineBreak
                || node instanceof HardLineBreak || node instanceof Image) {
            // handled as inline when parent mapped
            return null;
        }
        if (node instanceof Block block) {
            // fallback paragraph for unknown block
            return new Paragraph(mapInlineChildren(block));
        }
        return null;
    }

    private List<AdfNode> mapListChildren(com.vladsch.flexmark.ast.ListBlock list) {
        List<AdfNode> items = new ArrayList<>();
        for (Node child = list.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof com.vladsch.flexmark.ast.ListItem li) {
                items.add(mapListItem(li));
            }
        }
        return items;
    }

    private AdfNode mapListItem(com.vladsch.flexmark.ast.ListItem li) {
        List<AdfNode> paragraphs = new ArrayList<>();
        for (Node child = li.getFirstChild(); child != null; child = child.getNext()) {
            AdfNode mapped = mapNode(child);
            if (mapped != null) {
                paragraphs.add(mapped);
            }
        }
        return new com.tsystems.jira.adf.model.ListItem(paragraphs);
    }

    private boolean containsTaskItems(com.vladsch.flexmark.ast.ListBlock list) {
        for (Node child = list.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof TaskListItem) {
                return true;
            }
        }
        return false;
    }

    private List<AdfNode> mapTaskItems(com.vladsch.flexmark.ast.ListBlock list) {
        List<AdfNode> items = new ArrayList<>();
        for (Node child = list.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof TaskListItem tli) {
                items.add(mapTaskItem(tli));
            }
        }
        return items;
    }

    private TaskItem mapTaskItem(TaskListItem tli) {
        boolean done = tli.isItemDoneMarker();
        String state = done ? "DONE" : "TODO";
        List<AdfNode> inline = mapInlineChildren(tli);
        return new TaskItem(null, state, List.of(new Paragraph(inline)));
    }

    private List<AdfNode> mapInlineChildren(Node container) {
        List<AdfNode> inlines = new ArrayList<>();
        for (Node child = container == null ? null : container.getFirstChild(); child != null; child = child.getNext()) {
            inlines.addAll(mapInline(child, new ArrayList<>()));
        }
        if (inlines.isEmpty()) {
            inlines.add(new Text(""));
        }
        return inlines;
    }

    private List<AdfNode> mapInline(Node node, List<Mark> inherited) {
        List<AdfNode> result = new ArrayList<>();
        if (node instanceof com.vladsch.flexmark.ast.Text t) {
            Text txt = new Text(t.getChars().toString(), new ArrayList<>(inherited));
            result.add(txt);
            return result;
        }
        if (node instanceof StrongEmphasis se) {
            List<Mark> marks = new ArrayList<>(inherited);
            marks.add(Mark.strong());
            for (Node child = se.getFirstChild(); child != null; child = child.getNext()) {
                result.addAll(mapInline(child, marks));
            }
            return result;
        }
        if (node instanceof Emphasis em) {
            List<Mark> marks = new ArrayList<>(inherited);
            marks.add(Mark.em());
            for (Node child = em.getFirstChild(); child != null; child = child.getNext()) {
                result.addAll(mapInline(child, marks));
            }
            return result;
        }
        if (node instanceof Code code) {
            List<Mark> marks = new ArrayList<>(inherited);
            marks.add(Mark.code());
            Text txt = new Text(code.getText().toString(), marks);
            result.add(txt);
            return result;
        }
        if (node instanceof Strikethrough st) {
            List<Mark> marks = new ArrayList<>(inherited);
            marks.add(Mark.strike());
            for (Node child = st.getFirstChild(); child != null; child = child.getNext()) {
                result.addAll(mapInline(child, marks));
            }
            return result;
        }
        if (node instanceof Link link) {
            List<Mark> marks = new ArrayList<>(inherited);
            marks.add(Mark.link(link.getUrl().toString(), link.getTitle().toString()));
            for (Node child = link.getFirstChild(); child != null; child = child.getNext()) {
                result.addAll(mapInline(child, marks));
            }
            return result;
        }
        if (node instanceof AutoLink al) {
            List<Mark> marks = new ArrayList<>(inherited);
            marks.add(Mark.link(al.getUrl().toString(), null));
            Text txt = new Text(al.getUrl().toString(), marks);
            result.add(txt);
            return result;
        }
        if (node instanceof HardLineBreak || node instanceof SoftLineBreak) {
            result.add(new HardBreak());
            return result;
        }
        if (node instanceof Image img) {
            Media media = MediaUtil.fromImageSrc(img.getUrl().toString());
            result.add(new MediaInline(List.of(media)));
            return result;
        }
        if (node instanceof com.vladsch.flexmark.ext.emoji.Emoji emoji) {
            String shortName = emoji.getText().toString();
            result.add(new Emoji(shortName, null, emoji.getChars().toString()));
            return result;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            result.addAll(mapInline(child, inherited));
        }
        return result;
    }

    private Table mapTable(TableBlock block) {
        List<AdfNode> rows = new ArrayList<>();
        for (Node section = block.getFirstChild(); section != null; section = section.getNext()) {
            if (section instanceof TableHead head) {
                rows.addAll(mapTableSection(head, true));
            } else if (section instanceof TableBody body) {
                rows.addAll(mapTableSection(body, false));
            }
        }
        return new Table(rows);
    }

    private List<AdfNode> mapTableSection(Node section, boolean header) {
        List<AdfNode> rows = new ArrayList<>();
        for (Node row = section.getFirstChild(); row != null; row = row.getNext()) {
            if (row instanceof com.vladsch.flexmark.ext.tables.TableRow tr) {
                rows.add(mapTableRow(tr, header));
            }
        }
        return rows;
    }

    private AdfNode mapTableRow(com.vladsch.flexmark.ext.tables.TableRow tr, boolean header) {
        List<AdfNode> cells = new ArrayList<>();
        for (Node cell = tr.getFirstChild(); cell != null; cell = cell.getNext()) {
            if (cell instanceof com.vladsch.flexmark.ext.tables.TableCell tc) {
                cells.add(mapTableCell(tc, header));
            }
        }
        return new TableRow(cells);
    }

    private AdfNode mapTableCell(com.vladsch.flexmark.ext.tables.TableCell tc, boolean header) {
        List<AdfNode> content = mapInlineChildren(tc);
        if (header) {
            return new TableHeader(content, Map.of());
        }
        return new TableCell(content, Map.of());
    }
}
