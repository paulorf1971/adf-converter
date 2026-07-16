package com.tsystems.jira.adf.html;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

import com.tsystems.jira.adf.api.ConverterContext;
import com.tsystems.jira.adf.api.ConversionException;
import com.tsystems.jira.adf.api.OutboundConverter;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.model.AdfNode;
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
import com.tsystems.jira.adf.model.Mark;
import com.tsystems.jira.adf.model.Media;
import com.tsystems.jira.adf.model.MediaGroup;
import com.tsystems.jira.adf.model.MediaInline;
import com.tsystems.jira.adf.model.MediaSingle;
import com.tsystems.jira.adf.model.OrderedList;
import com.tsystems.jira.adf.model.Panel;
import com.tsystems.jira.adf.model.Paragraph;
import com.tsystems.jira.adf.model.Status;
import com.tsystems.jira.adf.model.Table;
import com.tsystems.jira.adf.model.TableCell;
import com.tsystems.jira.adf.model.TableCellBase;
import com.tsystems.jira.adf.model.TableHeader;
import com.tsystems.jira.adf.model.TableRow;
import com.tsystems.jira.adf.model.TaskItem;
import com.tsystems.jira.adf.model.TaskList;
import com.tsystems.jira.adf.model.Text;
import com.tsystems.jira.adf.util.MediaUtil;
import com.tsystems.jira.adf.util.TableUtil;

/**
 * Manual renderer for ADF → HTML.
 */
public class AdfToHtmlConverter implements OutboundConverter<String> {

    private final ConverterConfig config;

    public AdfToHtmlConverter(ConverterConfig config) {
        this.config = Optional.ofNullable(config).orElse(ConverterConfig.builder().build());
    }

    @Override
    public String convert(Document input, ConverterContext context) throws ConversionException {
        if (input == null) {
            return "";
        }
        Element root = new Element("div");
        renderChildren(input, root, context == null ? ConverterContext.builder().build() : context);
        org.jsoup.nodes.Document doc = new org.jsoup.nodes.Document("");
        doc.outputSettings(new OutputSettings().prettyPrint(false));
        root.childNodes().forEach(doc::appendChild);
        return doc.html();
    }

    private void renderChildren(AdfNode node, Element parent, ConverterContext ctx) {
        if (node.getContent() == null) {
            return;
        }
        for (AdfNode child : node.getContent()) {
            renderNode(child, parent, ctx);
        }
    }

    private void renderNode(AdfNode node, Element parent, ConverterContext ctx) {
        if (node == null) return;
        switch (node.getType()) {
            case "doc" -> renderChildren(node, parent, ctx);
            case "paragraph" -> {
                Element p = parent.appendElement("p");
                renderInlineChildren(node, p, ctx);
            }
            case "heading" -> {
                int level = asInt(node.getAttrs(), "level", 1);
                level = Math.max(1, Math.min(level, 6));
                Element h = parent.appendElement("h" + level);
                renderInlineChildren(node, h, ctx);
            }
            case "blockquote" -> {
                Element bq = parent.appendElement("blockquote");
                renderChildren(node, bq, ctx);
            }
            case "bulletList" -> {
                Element ul = parent.appendElement("ul");
                renderChildren(node, ul, ctx);
            }
            case "orderedList" -> {
                Element ol = parent.appendElement("ol");
                renderChildren(node, ol, ctx);
            }
            case "listItem" -> {
                Element li = parent.appendElement("li");
                renderChildren(node, li, ctx);
            }
            case "codeBlock" -> {
                Element pre = parent.appendElement("pre");
                Element code = pre.appendElement("code");
                String language = asString(node.getAttrs(), "language", "");
                if (!language.isBlank()) {
                    code.addClass("language-" + escapeAttr(language));
                }
                CodeBlock cb = (CodeBlock) node;
                if (cb.getText() != null) {
                    code.text(cb.getText());
                } else {
                    renderInlineChildren(node, code, ctx);
                }
            }
            case "hardBreak" -> parent.appendElement("br");
            case "text" -> appendText((Text) node, parent);
            case "panel" -> {
                Element div = parent.appendElement("div");
                div.attr("data-panel-type", asString(node.getAttrs(), "panelType", "info"));
                renderChildren(node, div, ctx);
            }
            case "status" -> appendStatus((Status) node, parent);
            case "taskList" -> {
                Element ul = parent.appendElement("ul");
                ul.addClass("task-list");
                renderChildren(node, ul, ctx);
            }
            case "taskItem" -> appendTaskItem((TaskItem) node, parent, ctx);
            case "decisionList" -> {
                Element ul = parent.appendElement("ul");
                ul.addClass("decision-list");
                renderChildren(node, ul, ctx);
            }
            case "decisionItem" -> {
                Element li = parent.appendElement("li");
                renderChildren(node, li, ctx);
            }
            case "table" -> renderTable(TableUtil.normalize((Table) node), parent, ctx);
            case "tableRow" -> renderTableRow((TableRow) node, parent, ctx);
            case "tableCell", "tableHeader" -> renderTableCell((TableCellBase) node, parent, ctx);
            case "media", "mediaSingle", "mediaGroup", "mediaInline" -> renderMedia(node, parent);
            case "emoji" -> appendEmoji((Emoji) node, parent);
            case "date" -> appendDate((DateNode) node, parent);
            default -> renderChildren(node, parent, ctx);
        }
    }

    private void renderInlineChildren(AdfNode node, Element parent, ConverterContext ctx) {
        if (node.getContent() == null) return;
        for (AdfNode child : node.getContent()) {
            if (child instanceof Text || child instanceof HardBreak || child instanceof MediaInline || child instanceof Emoji) {
                renderNode(child, parent, ctx);
            } else {
                renderNode(child, parent, ctx);
            }
        }
    }

    private void appendText(Text text, Element parent) {
        String value = Objects.toString(text.getText(), "");
        Element target = parent;
        if (text.getMarks() != null) {
            target = applyMarks(text.getMarks(), parent);
        }
        if (config.isEscapeHtml()) {
            target.appendText(value);
        } else {
            target.append(value);
        }
    }

    private Element applyMarks(List<Mark> marks, Element parent) {
        Element current = parent;
        for (Mark mark : marks) {
            switch (mark.getType()) {
                case "strong" -> current = current.appendElement("strong");
                case "em" -> current = current.appendElement("em");
                case "code" -> current = current.appendElement("code");
                case "strike" -> current = current.appendElement("s");
                case "underline" -> {
                    Element u = current.appendElement("span");
                    u.attr("style", "text-decoration:underline;");
                    current = u;
                }
                case "link" -> {
                    Element a = current.appendElement("a");
                    a.attr("href", escapeAttr(asString(mark.getAttrs(), "href", "")));
                    String title = asString(mark.getAttrs(), "title", null);
                    if (title != null) {
                        a.attr("title", escapeAttr(title));
                    }
                    current = a;
                }
                default -> {
                    if (!config.isAllowUnknownMarks()) {
                        throw new ConversionException("Unsupported mark: " + mark.getType());
                    }
                }
            }
        }
        return current;
    }

    private void appendStatus(Status status, Element parent) {
        Map<String, Object> attrs = status.getAttrs();
        String text = Objects.toString(attrs.getOrDefault("text", ""));
        String color = Objects.toString(attrs.getOrDefault("color", "default"));
        Element span = parent.appendElement("span");
        span.attr("data-status-color", escapeAttr(color));
        span.attr("data-status-text", escapeAttr(text));
        span.text(text);
    }

    private void appendTaskItem(TaskItem item, Element parent, ConverterContext ctx) {
        Element li = parent.appendElement("li");
        Element input = li.appendElement("input");
        input.attr("type", "checkbox");
        boolean done = Objects.equals("DONE", asString(item.getAttrs(), "state", ""));
        if (done) {
            input.attr("checked", "checked");
        }
        renderChildren(item, li, ctx);
    }

    private void renderTable(Table table, Element parent, ConverterContext ctx) {
        Element tbl = parent.appendElement("table");
        List<AdfNode> rows = table.getContent();
        if (rows == null) return;
        boolean header = !rows.isEmpty() && rows.get(0) instanceof TableRow tr && isHeaderRow(tr);
        if (header) {
            Element thead = tbl.appendElement("thead");
            renderTableRow((TableRow) rows.get(0), thead, ctx);
            rows = rows.subList(1, rows.size());
        }
        Element tbody = tbl.appendElement("tbody");
        for (AdfNode row : rows) {
            renderTableRow((TableRow) row, tbody, ctx);
        }
    }

    private boolean isHeaderRow(TableRow row) {
        return row.getContent() != null && row.getContent().stream().allMatch(c -> c instanceof TableHeader);
    }

    private void renderTableRow(TableRow row, Element parent, ConverterContext ctx) {
        Element tr = parent.appendElement("tr");
        if (row.getContent() != null) {
            for (AdfNode cell : row.getContent()) {
                renderTableCell((TableCellBase) cell, tr, ctx);
            }
        }
    }

    private void renderTableCell(TableCellBase cell, Element parent, ConverterContext ctx) {
        Element td = parent.appendElement(cell instanceof TableHeader ? "th" : "td");
        applyTableAttrs(td, cell.getAttrs());
        renderChildren(cell, td, ctx);
    }

    private void applyTableAttrs(Element td, Map<String, Object> attrs) {
        if (attrs == null) return;
        if (attrs.get("colspan") != null) td.attr("colspan", attrs.get("colspan").toString());
        if (attrs.get("rowspan") != null) td.attr("rowspan", attrs.get("rowspan").toString());
        String align = TableUtil.htmlAlign(attrs);
        if (align != null) td.attr("align", escapeAttr(align));
        String bgStyle = TableUtil.backgroundStyle(attrs);
        if (bgStyle != null) {
            String style = td.attr("style");
            if (!style.endsWith(";")) style = style + (style.isEmpty() ? "" : ";");
            td.attr("style", style + bgStyle);
        }
    }

    private void renderMedia(AdfNode node, Element parent) {
        Media media = asMedia(node);
        String id = asString(media.getAttrs(), "id", "");
        String type = asString(media.getAttrs(), "type", "file");
        String src = config.isMediaPlaceholder()
                ? MediaUtil.placeholder(id, type, asString(media.getAttrs(), "collection", null), config.getMediaBaseUrl())
                : MediaUtil.buildMediaUrl(config.getMediaBaseUrl(), media);
        Element img = parent.appendElement("img");
        img.attr("src", escapeAttr(src));
        img.attr("data-media-type", escapeAttr(type));
        if (media.getAttrs().get("collection") != null) {
            img.attr("data-media-collection", escapeAttr(media.getAttrs().get("collection").toString()));
        }
    }

    private Media asMedia(AdfNode node) {
        if (node instanceof Media m) return m;
        if (node instanceof MediaSingle ms && ms.getContent() != null && !ms.getContent().isEmpty() && ms.getContent().get(0) instanceof Media m) return m;
        if (node instanceof MediaGroup mg && mg.getContent() != null && !mg.getContent().isEmpty() && mg.getContent().get(0) instanceof Media m) return m;
        if (node instanceof MediaInline mi && mi.getContent() != null && !mi.getContent().isEmpty() && mi.getContent().get(0) instanceof Media m) return m;
        return new Media("media", "file", null, null, null);
    }

    private void appendEmoji(Emoji emoji, Element parent) {
        String shortName = asString(emoji.getAttrs(), "shortName", "");
        String text = asString(emoji.getAttrs(), "text", ":" + shortName + ":");
        Element span = parent.appendElement("span");
        if (!shortName.isBlank()) {
            span.attr("data-emoji-short-name", escapeAttr(shortName));
        }
        span.text(text);
    }

    private void appendDate(DateNode date, Element parent) {
        String ts = asString(date.getAttrs(), "timestamp", "");
        Element time = parent.appendElement("time");
        if (!ts.isBlank()) {
            String iso = toIso(ts);
            time.attr("datetime", escapeAttr(iso));
            time.text(iso);
        }
    }

    private String toIso(String ts) {
        try {
            return Instant.ofEpochMilli(Long.parseLong(ts)).toString();
        } catch (NumberFormatException | DateTimeParseException ex) {
            return ts;
        }
    }

    private int asInt(Map<String, Object> attrs, String key, int def) {
        Object v = attrs == null ? null : attrs.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    private String asString(Map<String, Object> attrs, String key, String def) {
        Object v = attrs == null ? null : attrs.get(key);
        return v == null ? def : v.toString();
    }

    private String escapeAttr(String value) {
        return Parser.unescapeEntities(org.jsoup.nodes.Entities.escape(value), true);
    }
}
