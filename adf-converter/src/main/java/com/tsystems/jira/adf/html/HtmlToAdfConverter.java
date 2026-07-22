package com.tsystems.jira.adf.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;

import com.tsystems.jira.adf.api.ConversionException;
import com.tsystems.jira.adf.api.ConverterContext;
import com.tsystems.jira.adf.api.InboundConverter;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.model.AdfNode;
import com.tsystems.jira.adf.model.Blockquote;
import com.tsystems.jira.adf.model.BulletList;
import com.tsystems.jira.adf.model.CodeBlock;
import com.tsystems.jira.adf.model.DateNode;
import com.tsystems.jira.adf.model.DecisionItem;
import com.tsystems.jira.adf.model.DecisionList;
import com.tsystems.jira.adf.model.Emoji;
import com.tsystems.jira.adf.model.HardBreak;
import com.tsystems.jira.adf.model.Heading;
import com.tsystems.jira.adf.model.ListItem;
import com.tsystems.jira.adf.model.Mark;
import com.tsystems.jira.adf.model.Media;
import com.tsystems.jira.adf.model.MediaInline;
import com.tsystems.jira.adf.model.MediaSingle;
import com.tsystems.jira.adf.model.OrderedList;
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
import com.tsystems.jira.adf.util.MediaUtil;
import com.tsystems.jira.adf.util.TableUtil;

public class HtmlToAdfConverter implements InboundConverter<String> {

    private final ConverterConfig config;

    public HtmlToAdfConverter(ConverterConfig config) {
        this.config = Optional.ofNullable(config).orElse(ConverterConfig.builder().build());
    }

    @Override
    public com.tsystems.jira.adf.model.Document convert(String input, ConverterContext context) throws ConversionException {
        if (input == null) {
            return new com.tsystems.jira.adf.model.Document(new ArrayList<>());
        }
        Document doc = Jsoup.parse(input, "", Parser.htmlParser());
        stripUnsafe(doc);
        List<AdfNode> content = new ArrayList<>();
        for (Node child : doc.body().childNodes()) {
            if (child instanceof Element el) {
                AdfNode mapped = mapBlock(el);
                if (mapped != null) {
                    content.add(mapped);
                }
            } else if (child instanceof TextNode tn && !tn.text().isBlank()) {
                content.add(new Paragraph(List.of(new Text(tn.text()))));
            }
        }
        return new com.tsystems.jira.adf.model.Document(1, content);
    }

    private void stripUnsafe(Document doc) {
        doc.select("script, style").remove();
    }

    private AdfNode mapBlock(Element el) {
        String tag = el.tagName();
        switch (tag) {
            case "p":
                return new Paragraph(mapInlineChildren(el));
            case "h1", "h2", "h3", "h4", "h5", "h6": {
                int level = Integer.parseInt(tag.substring(1));
                return new Heading(level, mapInlineChildren(el));
            }
            case "blockquote": {
                List<AdfNode> inner = new ArrayList<>();
                for (Node child : el.childNodes()) {
                    if (child instanceof Element cEl) {
                        AdfNode n = mapBlock(cEl);
                        if (n != null) inner.add(n);
                    }
                }
                if (inner.isEmpty()) inner.add(new Paragraph(mapInlineChildren(el)));
                return new Blockquote(inner);
            }
            case "ul": {
                if (el.hasClass("task-list")) {
                    return new TaskList(mapTaskItems(el));
                }
                if (el.hasClass("decision-list")) {
                    return new DecisionList(mapDecisionItems(el));
                }
                return new BulletList(mapListItems(el));
            }
            case "ol":
                return new OrderedList(mapListItems(el));
            case "pre": {
                Element code = el.selectFirst("code");
                String text = code != null ? code.text() : el.text();
                String lang = "";
                if (code != null) {
                    for (String cls : code.classNames()) {
                        if (cls.startsWith("language-")) {
                            lang = cls.substring("language-".length());
                            break;
                        }
                    }
                }
                return new CodeBlock(lang.isBlank() ? null : lang, text);
            }
            case "table":
                Table table = mapTable(el);
                return TableUtil.normalize(table);
            case "div": {
                if (el.hasAttr("data-panel-type")) {
                    return new Panel(el.attr("data-panel-type"), mapBlockChildren(el));
                }
                return mapFallbackBlock(el);
            }
            case "span": {
                if (el.hasAttr("data-status-color") || el.hasAttr("data-status-text")) {
                    return new Status(Map.of(
                            "text", el.attr("data-status-text"),
                            "color", el.attr("data-status-color")
                    ));
                }
                if (el.hasAttr("data-emoji-short-name")) {
                    return new Emoji(el.attr("data-emoji-short-name"), null, el.text());
                }
                return mapFallbackBlock(el);
            }
            case "img":
                return mapMedia(el);
            case "time":
                return new DateNode(el.hasAttr("data-adf-timestamp") ? el.attr("data-adf-timestamp") : el.attr("datetime"));
            case "br":
                return new HardBreak();
            case "li":
                return new ListItem(mapBlockChildren(el));
            default:
                return mapFallbackBlock(el);
        }
    }

    private AdfNode mapFallbackBlock(Element el) {
        return new Paragraph(mapInlineChildren(el));
    }

    private List<AdfNode> mapListItems(Element list) {
        List<AdfNode> items = new ArrayList<>();
        for (Element li : list.children()) {
            if (li.tagName().equals("li")) {
                items.add(new ListItem(mapBlockChildren(li)));
            }
        }
        return items;
    }

    private List<AdfNode> mapTaskItems(Element list) {
        List<AdfNode> items = new ArrayList<>();
        for (Element li : list.children()) {
            if (!li.tagName().equals("li")) continue;
            Element checkbox = li.selectFirst("input[type=checkbox]");
            boolean done = checkbox != null && checkbox.hasAttr("checked");
            String state = done ? "DONE" : "TODO";
            items.add(new TaskItem(null, state, mapBlockChildren(li)));
        }
        return items;
    }

    private List<AdfNode> mapDecisionItems(Element list) {
        List<AdfNode> items = new ArrayList<>();
        for (Element li : list.children()) {
            if (!li.tagName().equals("li")) continue;
            items.add(new DecisionItem(li.id(), mapBlockChildren(li)));
        }
        return items;
    }

    private List<AdfNode> mapBlockChildren(Element el) {
        List<AdfNode> blocks = new ArrayList<>();
        List<AdfNode> inline = new ArrayList<>();
        for (Node child : el.childNodes()) {
            if (child instanceof Element cEl) {
                if (cEl.tagName().equals("input")) {
                    continue;
                }
                if (!isBlockElement(cEl)) {
                    inline.addAll(mapInline(cEl, new ArrayList<>()));
                    continue;
                }
                if (!inline.isEmpty()) {
                    blocks.add(new Paragraph(inline));
                    inline = new ArrayList<>();
                }
                AdfNode n = mapBlock(cEl);
                if (n != null) blocks.add(n);
            } else if (child instanceof TextNode tn && !tn.text().isBlank()) {
                inline.add(new Text(tn.text()));
            }
        }
        if (!inline.isEmpty()) {
            blocks.add(new Paragraph(inline));
        }
        if (blocks.isEmpty()) {
            blocks.add(new Paragraph(mapInlineChildren(el)));
        }
        return blocks;
    }

    private boolean isBlockElement(Element el) {
        return switch (el.tagName()) {
            case "p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "ul", "ol", "pre", "table", "div" -> true;
            default -> false;
        };
    }

    private List<AdfNode> mapInlineChildren(Element el) {
        List<AdfNode> result = new ArrayList<>();
        for (Node child : el.childNodes()) {
            result.addAll(mapInline(child, new ArrayList<>()));
        }
        if (result.isEmpty()) result.add(new Text(""));
        return result;
    }

    private List<AdfNode> mapInline(Node node, List<Mark> inherited) {
        List<AdfNode> out = new ArrayList<>();
        if (node instanceof TextNode tn) {
            out.add(new Text(tn.text(), new ArrayList<>(inherited)));
            return out;
        }
        if (node instanceof Element el) {
            String tag = el.tagName();
            switch (tag) {
                case "strong", "b" -> {
                    List<Mark> marks = new ArrayList<>(inherited); marks.add(Mark.strong());
                    el.childNodes().forEach(c -> out.addAll(mapInline(c, marks)));
                    return out;
                }
                case "em", "i" -> {
                    List<Mark> marks = new ArrayList<>(inherited); marks.add(Mark.em());
                    el.childNodes().forEach(c -> out.addAll(mapInline(c, marks)));
                    return out;
                }
                case "code" -> {
                    List<Mark> marks = new ArrayList<>(inherited); marks.add(Mark.code());
                    el.childNodes().forEach(c -> out.addAll(mapInline(c, marks)));
                    return out;
                }
                case "s", "strike" -> {
                    List<Mark> marks = new ArrayList<>(inherited); marks.add(Mark.strike());
                    el.childNodes().forEach(c -> out.addAll(mapInline(c, marks)));
                    return out;
                }
                case "span" -> {
                    if (el.hasAttr("data-emoji-short-name")) {
                        out.add(new Emoji(el.attr("data-emoji-short-name"), null, el.text()));
                        return out;
                    }
                    if (el.hasAttr("style") && el.attr("style").toLowerCase().contains("underline")) {
                        List<Mark> marks = new ArrayList<>(inherited); marks.add(Mark.underline());
                        el.childNodes().forEach(c -> out.addAll(mapInline(c, marks)));
                        return out;
                    }
                    el.childNodes().forEach(c -> out.addAll(mapInline(c, inherited)));
                    return out;
                }
                case "a" -> {
                    List<Mark> marks = new ArrayList<>(inherited);
                    marks.add(Mark.link(el.attr("href"), el.hasAttr("title") ? el.attr("title") : null));
                    el.childNodes().forEach(c -> out.addAll(mapInline(c, marks)));
                    return out;
                }
                case "br" -> { out.add(new HardBreak()); return out; }
                case "img" -> { out.add(mapMediaInline(el)); return out; }
                case "time" -> { out.add(new DateNode(el.hasAttr("data-adf-timestamp") ? el.attr("data-adf-timestamp") : el.attr("datetime"))); return out; }
                default -> el.childNodes().forEach(c -> out.addAll(mapInline(c, inherited)));
            }
        }
        return out;
    }

    private Table mapTable(Element table) {
        List<AdfNode> rows = new ArrayList<>();
        Element thead = table.selectFirst("thead");
        Element tbody = table.selectFirst("tbody");
        if (thead != null) rows.addAll(mapTableSection(thead, true));
        if (tbody != null) rows.addAll(mapTableSection(tbody, false));
        if (thead == null && tbody == null) rows.addAll(mapTableSection(table, true));
        return new Table(rows);
    }

    private List<AdfNode> mapTableSection(Element section, boolean headerDefault) {
        List<AdfNode> rows = new ArrayList<>();
        for (Element tr : section.select("> tr")) {
            List<AdfNode> cells = new ArrayList<>();
            for (Element cell : tr.children()) {
                boolean header = cell.tagName().equals("th") || (headerDefault && cell.tagName().equals("td"));
                cells.add(mapTableCell(cell, header));
            }
            rows.add(new TableRow(cells));
        }
        return rows;
    }

    private AdfNode mapTableCell(Element cell, boolean header) {
        java.util.Map<String, Object> attrs = new java.util.HashMap<>();
        Integer colspan = attrInt(cell, "colspan");
        Integer rowspan = attrInt(cell, "rowspan");
        String background = styleValue(cell.attr("style"), "background");
        String align = cell.hasAttr("align") ? cell.attr("align") : null;
        if (colspan != null) attrs.put("colspan", colspan);
        if (rowspan != null) attrs.put("rowspan", rowspan);
        if (background != null) attrs.put("background", background);
        if (align != null && !align.isBlank()) attrs.put("align", align);
        List<AdfNode> content = List.of(new Paragraph(mapInlineChildren(cell)));
        return header ? new TableHeader(content, attrs) : new TableCell(content, attrs);
    }

    private Integer attrInt(Element el, String attr) {
        if (!el.hasAttr(attr)) return null;
        try { return Integer.parseInt(el.attr(attr)); } catch (NumberFormatException e) { return null; }
    }

    private String styleValue(String style, String key) {
        if (style == null) return null;
        String[] parts = style.split(";");
        for (String p : parts) {
            if (p.trim().startsWith(key)) {
                String[] kv = p.split(":", 2);
                if (kv.length == 2) return kv[1].trim();
            }
        }
        return null;
    }

    private AdfNode mapMedia(Element img) {
        Media media = MediaUtil.fromImageSrc(img.attr("src"));
        if (img.hasAttr("data-media-collection")) {
            media.getAttrs().put("collection", img.attr("data-media-collection"));
        }
        if (img.hasAttr("data-media-type")) {
            media.getAttrs().put("type", img.attr("data-media-type"));
        }
        return new MediaSingle(List.of(media));
    }

    private AdfNode mapMediaInline(Element img) {
        Media media = MediaUtil.fromImageSrc(img.attr("src"));
        if (img.hasAttr("data-media-collection")) {
            media.getAttrs().put("collection", img.attr("data-media-collection"));
        }
        if (img.hasAttr("data-media-type")) {
            media.getAttrs().put("type", img.attr("data-media-type"));
        }
        return new MediaInline(List.of(media));
    }
}
