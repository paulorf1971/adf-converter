package com.tsystems.jira.adf.markdown;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

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
import com.tsystems.jira.adf.model.Media;
import com.tsystems.jira.adf.model.MediaGroup;
import com.tsystems.jira.adf.model.MediaInline;
import com.tsystems.jira.adf.model.MediaSingle;
import com.tsystems.jira.adf.util.MediaUtil;
import com.tsystems.jira.adf.util.TableUtil;

/**
 * Manual renderer for ADF → Markdown.
 */
public class AdfToMarkdownConverter implements OutboundConverter<String> {

    private final ConverterConfig config;

    public AdfToMarkdownConverter(ConverterConfig config) {
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

        Consumer<AdfNode> fallback = n -> renderChildren(n, sb, state, ctx);

        switch (node.getType()) {
            case "doc" -> {
                renderChildren(node, sb, state, ctx);
            }
            case "paragraph" -> {
                int startLen = sb.length();
                renderChildren(node, sb, state, ctx);
                trimTrailingSpace(sb);
                if (sb.length() > startLen) {
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
                state.pushPrefix("> ");
                renderChildren(node, sb, state, ctx);
                state.popPrefix();
                trimTrailingSpace(sb);
                sb.append("\n\n");
            }
            case "bulletList" -> {
                state.pushList(new ListState("* ", 0));
                renderChildren(node, sb, state, ctx);
                state.popList();
                sb.append("\n");
            }
            case "orderedList" -> {
                state.pushList(new ListState("%d. ", 1));
                renderChildren(node, sb, state, ctx);
                state.popList();
                sb.append("\n");
            }
            case "listItem" -> {
                if (!state.hasList()) {
                    sb.append("- ");
                    renderChildren(node, sb, state, ctx);
                    sb.append("\n");
                } else {
                    ListState ls = state.peekList();
                    String prefix = ls.format();
                    String[] lines = renderToTemp(node, state, ctx).split("\n");
                    sb.append(prefix).append(lines.length > 0 ? lines[0] : "");
                    sb.append("\n");
                    for (int i = 1; i < lines.length; i++) {
                        sb.append(" ".repeat(prefix.length())).append(lines[i]).append("\n");
                    }
                    ls.increment();
                }
            }
            case "codeBlock" -> {
                CodeBlock cb = (CodeBlock) node;
                String language = asString(node.getAttrs(), "language", null);
                sb.append("```");
                if (language != null && !language.isBlank()) {
                    sb.append(language);
                }
                sb.append("\n");
                if (cb.getText() != null) {
                    sb.append(cb.getText());
                } else {
                    renderChildren(node, sb, state, ctx);
                }
                sb.append("\n```").append("\n\n");
            }
            case "hardBreak" -> sb.append("  \n");
            case "text" -> renderText((Text) node, sb, state, ctx);
            case "panel" -> renderPanel((Panel) node, sb, state, ctx);
            case "status" -> renderStatus((Status) node, sb);
            case "taskList" -> renderTaskList((TaskList) node, sb, state, ctx);
            case "taskItem" -> renderTaskItem((TaskItem) node, sb, state, ctx);
            case "decisionList" -> renderDecisionList((DecisionList) node, sb, state, ctx);
            case "decisionItem" -> renderDecisionItem((DecisionItem) node, sb, state, ctx);
            case "table" -> renderTable(TableUtil.normalize((Table) node), sb, state, ctx);
            case "tableRow" -> renderChildren(node, sb, state, ctx);
            case "tableCell", "tableHeader" -> renderTableCell(node, sb, state, ctx);
            case "media", "mediaSingle", "mediaGroup", "mediaInline" -> renderMedia(node, sb);
            case "emoji" -> renderEmoji((Emoji) node, sb);
            case "date" -> renderDate((DateNode) node, sb);
            default -> fallback.accept(node);
        }
    }

    private void renderPanel(Panel panel, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        state.pushPrefix("> ");
        renderChildren(panel, sb, state, ctx);
        state.popPrefix();
        sb.append("\n\n");
    }

    private void renderStatus(Status status, StringBuilder sb) {
        Map<String, Object> attrs = status.getAttrs();
        String text = Objects.toString(attrs.getOrDefault("text", "status"));
        String color = Objects.toString(attrs.getOrDefault("color", "default"));
        sb.append("[`status:").append(escapeInline(text)).append("|color=").append(color).append("]");
    }

    private void renderTaskList(TaskList list, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        state.pushList(new ListState("- [ ] ", 0, true));
        renderChildren(list, sb, state, ctx);
        state.popList();
        sb.append("\n");
    }

    private void renderTaskItem(TaskItem item, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        boolean done = Objects.equals("DONE", asString(item.getAttrs(), "state", ""));
        String prefix = done ? "- [x] " : "- [ ] ";
        String[] lines = renderToTemp(item, state, ctx).split("\n");
        if (lines.length > 0) {
            sb.append(prefix).append(lines[0]).append("\n");
        }
        for (int i = 1; i < lines.length; i++) {
            sb.append(" ".repeat(prefix.length())).append(lines[i]).append("\n");
        }
    }

    private void renderDecisionList(DecisionList list, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        state.pushList(new ListState("* ", 0));
        renderChildren(list, sb, state, ctx);
        state.popList();
        sb.append("\n");
    }

    private void renderDecisionItem(DecisionItem item, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        String[] lines = renderToTemp(item, state, ctx).split("\n");
        String prefix = "* ";
        if (lines.length > 0) {
            sb.append(prefix).append(lines[0]).append("\n");
        }
        for (int i = 1; i < lines.length; i++) {
            sb.append(" ".repeat(prefix.length())).append(lines[i]).append("\n");
        }
    }

    private void renderTable(Table table, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        List<AdfNode> rows = table.getContent();
        if (rows == null) {
            return;
        }
        boolean headerRow = !rows.isEmpty() && isHeaderRow(rows.get(0));
        for (AdfNode row : rows) {
            renderChildren(row, sb, state, ctx);
            sb.append("|");
            sb.append("\n");
            if (headerRow) {
                List<AdfNode> cells = row.getContent();
                if (cells != null) {
                    sb.append("|");
                    for (AdfNode ignored : cells) {
                        sb.append(" --- |");
                    }
                    sb.append("\n");
                }
                headerRow = false;
            }
        }
        sb.append("\n");
    }

    private void renderTableCell(AdfNode cell, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        sb.append("|");
        sb.append(" ");
        String rendered = renderToTemp(cell, state, ctx).replace("\n", "<br>");
        sb.append(rendered);
        sb.append(" ");
    }

    private boolean isHeaderRow(AdfNode row) {
        if (row == null || row.getContent() == null) {
            return false;
        }
        return row.getContent().stream().allMatch(c -> c instanceof TableHeader);
    }

    private void renderMedia(AdfNode node, StringBuilder sb) {
        Media media = asMedia(node);
        String id = asString(media.getAttrs(), "id", "media");
        String type = asString(media.getAttrs(), "type", "file");
        String url = config.isMediaPlaceholder()
                ? MediaUtil.placeholder(id, type, asString(media.getAttrs(), "collection", null), config.getMediaBaseUrl())
                : MediaUtil.buildMediaUrl(config.getMediaBaseUrl(), media);
        sb.append("![").append(type).append("](").append(url).append(")");
    }

    private Media asMedia(AdfNode node) {
        if (node instanceof Media m) return m;
        if (node instanceof MediaSingle ms && ms.getContent() != null && !ms.getContent().isEmpty() && ms.getContent().get(0) instanceof Media m) return m;
        if (node instanceof MediaGroup mg && mg.getContent() != null && !mg.getContent().isEmpty() && mg.getContent().get(0) instanceof Media m) return m;
        if (node instanceof MediaInline mi && mi.getContent() != null && !mi.getContent().isEmpty() && mi.getContent().get(0) instanceof Media m) return m;
        return new Media("media", "file", null, null, null);
    }

    private void renderEmoji(Emoji emoji, StringBuilder sb) {
        String shortName = asString(emoji.getAttrs(), "shortName", null);
        String text = asString(emoji.getAttrs(), "text", null);
        if (shortName != null) {
            sb.append(":").append(shortName.replace(":", "")).append(":");
        } else if (text != null) {
            sb.append(text);
        }
    }

    private void renderDate(DateNode date, StringBuilder sb) {
        String ts = asString(date.getAttrs(), "timestamp", "");
        sb.append(ts);
    }

    private void renderText(Text text, StringBuilder sb, RenderingState state, ConverterContext ctx) {
        String value = escapeInline(text.getText());
        if (text.getMarks() != null && !text.getMarks().isEmpty()) {
            value = applyMarks(text.getMarks(), value, ctx);
        }
        sb.append(state.renderPrefixes()).append(value);
    }

    private String applyMarks(List<Mark> marks, String value, ConverterContext ctx) {
        for (Mark mark : marks) {
            switch (mark.getType()) {
                case "strong" -> value = "**" + value + "**";
                case "em" -> value = "_" + value + "_";
                case "code" -> value = "`" + value + "`";
                case "strike" -> value = "~~" + value + "~~";
                case "underline" -> value = "<u>" + value + "</u>";
                case "link" -> {
                    String href = asString(mark.getAttrs(), "href", "");
                    value = "[" + value + "](" + escapeUrl(href) + ")";
                }
                default -> {
                    if (!config.isAllowUnknownMarks()) {
                        throw new ConversionException("Unsupported mark: " + mark.getType());
                    }
                }
            }
        }
        return value;
    }

    private String renderToTemp(AdfNode node, RenderingState state, ConverterContext ctx) {
        StringBuilder tmp = new StringBuilder();
        RenderingState nested = state.copy();
        renderChildren(node, tmp, nested, ctx);
        return tmp.toString().trim();
    }

    private String escapeInline(String text) {
        if (text == null) return "";
        String escaped = text
                .replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("!", "\\!");
        if (config.isEscapeHtml()) {
            escaped = escaped
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }
        return escaped;
    }

    private String escapeUrl(String url) {
        if (url == null) return "";
        return url.replace("(", "%28").replace(")", "%29");
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

    private void trimTrailingSpace(StringBuilder sb) {
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
            sb.setLength(sb.length() - 1);
        }
    }

    private String normalize(String s) {
        return s.replaceAll("\n{3,}", "\n\n").trim();
    }

    /** Rendering state for list/prefix management. */
    private static class RenderingState {
        private final Deque<String> prefixes = new ArrayDeque<>();
        private final Deque<ListState> lists = new ArrayDeque<>();

        RenderingState copy() {
            RenderingState c = new RenderingState();
            c.prefixes.addAll(this.prefixes);
            for (ListState ls : this.lists) {
                c.lists.addLast(ls.copy());
            }
            return c;
        }

        void pushPrefix(String p) {
            prefixes.addLast(p);
        }

        void popPrefix() {
            if (!prefixes.isEmpty()) {
                prefixes.removeLast();
            }
        }

        String renderPrefixes() {
            StringBuilder sb = new StringBuilder();
            prefixes.forEach(sb::append);
            return sb.toString();
        }

        void pushList(ListState state) {
            lists.addLast(state);
        }

        void popList() {
            if (!lists.isEmpty()) {
                lists.removeLast();
            }
        }

        boolean hasList() {
            return !lists.isEmpty();
        }

        ListState peekList() {
            return lists.peekLast();
        }
    }

    private static class ListState {
        private final String pattern;
        private int counter;
        private final boolean fixed;

        ListState(String pattern, int start) {
            this(pattern, start, false);
        }

        ListState(String pattern, int start, boolean fixed) {
            this.pattern = pattern;
            this.counter = start;
            this.fixed = fixed;
        }

        String format() {
            if (fixed || !pattern.contains("%")) {
                return pattern;
            }
            return String.format(pattern, counter);
        }

        void increment() {
            counter++;
        }

        ListState copy() {
            return new ListState(this.pattern, this.counter, this.fixed);
        }
    }
}
