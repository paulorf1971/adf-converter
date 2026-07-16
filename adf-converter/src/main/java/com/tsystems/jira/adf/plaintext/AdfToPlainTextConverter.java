package com.tsystems.jira.adf.plaintext;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.tsystems.jira.adf.api.ConverterContext;
import com.tsystems.jira.adf.api.ConversionException;
import com.tsystems.jira.adf.api.OutboundConverter;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.model.AdfNode;
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
import com.tsystems.jira.adf.model.TableRow;
import com.tsystems.jira.adf.model.TaskItem;
import com.tsystems.jira.adf.model.TaskList;
import com.tsystems.jira.adf.model.Text;
import com.tsystems.jira.adf.util.MediaUtil;
import com.tsystems.jira.adf.util.TableUtil;

/**
 * Manual renderer for ADF → Plain Text.
 */
public class AdfToPlainTextConverter implements OutboundConverter<String> {

    private final ConverterConfig config;

    public AdfToPlainTextConverter(ConverterConfig config) {
        this.config = Optional.ofNullable(config).orElse(ConverterConfig.builder().build());
    }

    @Override
    public String convert(Document input, ConverterContext context) throws ConversionException {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        RenderingState state = new RenderingState();
        renderNode(input, sb, state, context == null ? ConverterContext.builder().build() : context);
        return normalize(sb.toString());
    }

    private void renderChildren(AdfNode node, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        if (node.getContent() == null) {
            return;
        }
        for (AdfNode child : node.getContent()) {
            renderNode(child, sb, state, ctx);
        }
    }

    private void renderNode(AdfNode node, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        if (node == null) {
            return;
        }

        switch (node.getType()) {
            case "doc" -> renderChildren(node, sb, state, ctx);
            case "paragraph" -> {
                int start = sb.length();
                renderChildren(node, sb, state, ctx);
                trimTrailingNewlines(sb);
                if (sb.length() > start) {
                    sb.append("\n\n");
                }
            }
            case "heading" -> {
                int level = asInt(node.getAttrs(), "level", 1);
                level = Math.max(1, Math.min(level, 6));
                sb.append("#".repeat(level)).append(" ");
                renderChildren(node, sb, state, ctx);
                sb.append("\n\n");
            }
            case "blockquote" -> {
                String rendered = renderToTemp(node, state, ctx);
                sb.append(prefixLines(rendered, "> "));
                sb.append("\n\n");
            }
            case "bulletList" -> {
                state.pushList(new ListState(ListKind.BULLET, state.currentIndent()));
                renderChildren(node, sb, state, ctx);
                state.popList();
                sb.append("\n");
            }
            case "orderedList" -> {
                int start = asInt(node.getAttrs(), "order", 1);
                state.pushList(new ListState(ListKind.ORDERED, state.currentIndent(), start));
                renderChildren(node, sb, state, ctx);
                state.popList();
                sb.append("\n");
            }
            case "taskList" -> {
                state.pushList(new ListState(ListKind.TASK, state.currentIndent()));
                renderChildren(node, sb, state, ctx);
                state.popList();
                sb.append("\n");
            }
            case "decisionList" -> {
                state.pushList(new ListState(ListKind.DECISION, state.currentIndent()));
                renderChildren(node, sb, state, ctx);
                state.popList();
                sb.append("\n");
            }
            case "listItem" -> renderListItem(node, sb, state, ctx);
            case "taskItem" -> renderListItem(node, sb, state, ctx);
            case "decisionItem" -> renderListItem(node, sb, state, ctx);
            case "codeBlock" -> renderCodeBlock((CodeBlock) node, sb, state, ctx);
            case "hardBreak" -> sb.append("\n");
            case "text" -> renderText((Text) node, sb, state, ctx);
            case "panel" -> renderPanel((Panel) node, sb, state, ctx);
            case "status" -> renderStatus((Status) node, sb);
            case "table" -> renderTable(TableUtil.normalize((Table) node), sb, state, ctx);
            case "tableRow" -> renderChildren(node, sb, state, ctx);
            case "tableCell", "tableHeader" -> sb.append(renderToTemp(node, state, ctx));
            case "media", "mediaSingle", "mediaGroup", "mediaInline" -> renderMedia(node, sb);
            case "emoji" -> renderEmoji((Emoji) node, sb);
            case "date" -> renderDate((DateNode) node, sb);
            default -> renderChildren(node, sb, state, ctx);
        }
    }

    private void renderCodeBlock(CodeBlock cb, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        String language = asString(cb.getAttrs(), "language", "");
        sb.append("```");
        if (!language.isBlank()) {
            sb.append(language);
        }
        sb.append("\n");
        if (cb.getText() != null) {
            sb.append(cb.getText());
        } else {
            renderChildren(cb, sb, state, ctx);
        }
        sb.append("\n```").append("\n\n");
    }

    private void renderListItem(AdfNode node, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        ListState ls = state.peekList();
        if (ls == null) {
            // fallback: treat as paragraph
            renderChildren(node, sb, state, ctx);
            sb.append("\n");
            return;
        }
        String rendered = renderToTemp(node, state, ctx);
        String[] lines = rendered.split("\n", -1);
        String prefix = ls.prefixFor(node);
        sb.append(prefix).append(lines.length > 0 ? lines[0] : "").append("\n");
        for (int i = 1; i < lines.length; i++) {
            sb.append(" ".repeat(prefix.length())).append(lines[i]).append("\n");
        }
        ls.increment();
    }

    private void renderText(Text text, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        String value = Objects.toString(text.getText(), "");
        if (text.getMarks() != null && !text.getMarks().isEmpty()) {
            value = applyMarks(text.getMarks(), value, ctx);
        }
        sb.append(value);
    }

    private void renderPanel(Panel panel, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        String type = asString(panel.getAttrs(), "panelType", "info");
        String inner = renderToTemp(panel, state, ctx);
        sb.append("[PANEL ").append(type).append("] ").append(inner).append("\n\n");
    }

    private void renderStatus(Status status, StringBuilder sb) {
        String text = Objects.toString(status.getAttrs().getOrDefault("text", "status"));
        String color = Objects.toString(status.getAttrs().getOrDefault("color", "default"));
        sb.append("[STATUS ").append(color).append("] ").append(text);
    }

    private void renderTable(Table table, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        List<AdfNode> rows = table.getContent();
        if (rows == null) {
            return;
        }
        for (AdfNode row : rows) {
            if (row instanceof TableRow tr && tr.getContent() != null) {
                StringBuilder rowSb = new StringBuilder();
                boolean first = true;
                for (AdfNode cell : tr.getContent()) {
                    if (!first) {
                        rowSb.append(" | ");
                    }
                    String rendered = renderToTemp(cell, state, ctx).replace("\n", " ").trim();
                    rowSb.append(rendered);
                    first = false;
                }
                sb.append(rowSb).append("\n");
            }
        }
        sb.append("\n");
    }

    private void renderMedia(AdfNode node, StringBuilder sb) {
        Media media = asMedia(node);
        String id = asString(media.getAttrs(), "id", "media");
        String placeholder = MediaUtil.placeholder(id, asString(media.getAttrs(), "type", "file"), asString(media.getAttrs(), "collection", null), config.getMediaBaseUrl());
        if (config.isMediaPlaceholder()) {
            sb.append("[").append(placeholder).append("]");
        } else {
            sb.append(MediaUtil.buildMediaUrl(config.getMediaBaseUrl(), media));
        }
    }

    private void renderEmoji(Emoji emoji, StringBuilder sb) {
        String text = asString(emoji.getAttrs(), "text", "");
        String shortName = asString(emoji.getAttrs(), "shortName", "");
        if (!text.isBlank()) {
            sb.append(text);
        } else if (!shortName.isBlank()) {
            sb.append(shortName);
        }
    }

    private void renderDate(DateNode date, StringBuilder sb) {
        String ts = asString(date.getAttrs(), "timestamp", "");
        sb.append(toIso(ts));
    }

    private String toIso(String ts) {
        try {
            return Instant.ofEpochMilli(Long.parseLong(ts)).toString();
        } catch (NumberFormatException | DateTimeParseException ex) {
            return ts;
        }
    }

    private Media asMedia(AdfNode node) {
        if (node instanceof Media m) return m;
        if (node instanceof MediaSingle ms && ms.getContent() != null && !ms.getContent().isEmpty() && ms.getContent().get(0) instanceof Media m) return m;
        if (node instanceof MediaGroup mg && mg.getContent() != null && !mg.getContent().isEmpty() && mg.getContent().get(0) instanceof Media m) return m;
        if (node instanceof MediaInline mi && mi.getContent() != null && !mi.getContent().isEmpty() && mi.getContent().get(0) instanceof Media m) return m;
        return new Media("media", "file", null, null, null);
    }

    private String applyMarks(List<Mark> marks, String value, ConverterContext ctx) {
        String result = value;
        for (Mark mark : marks) {
            switch (mark.getType()) {
                case "link" -> {
                    String href = asString(mark.getAttrs(), "href", "");
                    if (!href.isBlank()) {
                        result = result + " (" + href + ")";
                    }
                }
                case "code" -> {
                    // keep as-is
                }
                default -> {
                    if (!config.isAllowUnknownMarks()) {
                        // ignore silently for plain text
                    }
                }
            }
        }
        return result;
    }

    private String renderToTemp(AdfNode node, RenderingState state, ConverterContext ctx) {
        StringBuilder tmp = new StringBuilder();
        RenderingState nested = state.copy();
        renderChildren(node, tmp, nested, ctx);
        return tmp.toString().trim();
    }

    private String prefixLines(String text, String prefix) {
        String[] lines = text.split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            out.append(prefix).append(lines[i]);
            if (i < lines.length - 1) {
                out.append("\n");
            }
        }
        return out.toString();
    }

    private void trimTrailingNewlines(StringBuilder sb) {
        while (sb.length() > 0 && (sb.charAt(sb.length() - 1) == '\n' || sb.charAt(sb.length() - 1) == '\r')) {
            sb.setLength(sb.length() - 1);
        }
    }

    private String normalize(String s) {
        String normalized = s.replaceAll("\n{3,}", "\n\n");
        while (normalized.endsWith("\n")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private int asInt(Map<String, Object> attrs, String key, int def) {
        Object v = attrs == null ? null : attrs.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private String asString(Map<String, Object> attrs, String key, String def) {
        Object v = attrs == null ? null : attrs.get(key);
        return v == null ? def : v.toString();
    }

    /** List rendering kind. */
    private enum ListKind { BULLET, ORDERED, TASK, DECISION }

    /** Rendering state for list indentation. */
    private static class RenderingState {
        private final Deque<ListState> lists = new ArrayDeque<>();

        RenderingState copy() {
            RenderingState c = new RenderingState();
            for (ListState ls : this.lists) {
                c.lists.addLast(ls.copy());
            }
            return c;
        }

        void pushList(ListState state) {
            lists.addLast(state);
        }

        void popList() {
            if (!lists.isEmpty()) {
                lists.removeLast();
            }
        }

        ListState peekList() {
            return lists.peekLast();
        }

        int currentIndent() {
            return lists.size() * 2;
        }
    }

    private static class ListState {
        private final ListKind kind;
        private final int indent;
        private int counter;

        ListState(ListKind kind, int indent) {
            this(kind, indent, 1);
        }

        ListState(ListKind kind, int indent, int start) {
            this.kind = kind;
            this.indent = indent;
            this.counter = start;
        }

        String prefixFor(AdfNode item) {
            String base;
            if (kind == ListKind.ORDERED) {
                base = counter + ". ";
            } else if (kind == ListKind.TASK) {
                boolean done = item instanceof TaskItem && Objects.equals("DONE", ((TaskItem) item).getAttrs().get("state"));
                base = done ? "- [x] " : "- [ ] ";
            } else {
                base = "- ";
            }
            return " ".repeat(indent) + base;
        }

        void increment() {
            if (kind == ListKind.ORDERED) {
                counter++;
            }
        }

        ListState copy() {
            return new ListState(this.kind, this.indent, this.counter);
        }
    }
}
