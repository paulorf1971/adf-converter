package com.tsystems.jira.adf.plaintext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tsystems.jira.adf.api.ConverterContext;
import com.tsystems.jira.adf.api.ConversionException;
import com.tsystems.jira.adf.api.InboundConverter;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.model.AdfNode;
import com.tsystems.jira.adf.model.BulletList;
import com.tsystems.jira.adf.model.CodeBlock;
import com.tsystems.jira.adf.model.Document;
import com.tsystems.jira.adf.model.HardBreak;
import com.tsystems.jira.adf.model.Media;
import com.tsystems.jira.adf.model.MediaSingle;
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

/**
 * Minimal Plain Text → ADF parser.
 */
public class PlainTextToAdfConverter implements InboundConverter<String> {

    private static final Pattern ORDERED_PATTERN = Pattern.compile("^(\\s*)(\\d+)\\. (.*)$");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^(\\s*)([-*]) (.*)$");
    private static final Pattern TASK_PATTERN = Pattern.compile("^(\\s*)- \\[( |x|X)\\] (.*)$");
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("^```(.*)$");
    private static final Pattern MEDIA_PATTERN = Pattern.compile("^\\[media:([^]]+)]$");

    private final ConverterConfig config;

    public PlainTextToAdfConverter(ConverterConfig config) {
        this.config = Optional.ofNullable(config).orElse(ConverterConfig.builder().build());
    }

    @Override
    public Document convert(String input, ConverterContext context) throws ConversionException {
        if (input == null) {
            return new Document(new ArrayList<>());
        }

        List<AdfNode> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        boolean inCodeFence = false;
        String normalizedInput = input.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalizedInput.split("\n", -1)) {
            if (CODE_FENCE_PATTERN.matcher(line).matches()) {
                current.add(line);
                if (inCodeFence) {
                    blocks.addAll(parseBlocks(String.join("\n", current)));
                    current.clear();
                }
                inCodeFence = !inCodeFence;
                continue;
            }
            if (!inCodeFence && line.isBlank()) {
                if (!current.isEmpty()) {
                    blocks.addAll(parseBlocks(String.join("\n", current)));
                    current.clear();
                }
                continue;
            }
            current.add(line);
        }
        if (!current.isEmpty()) {
            blocks.addAll(parseBlocks(String.join("\n", current)));
        }
        return new Document(1, blocks);
    }

    private List<AdfNode> parseBlocks(String para) {
        List<String> lines = List.of(para.split("\\n", -1));
        if (isCodeFence(lines)) {
            return List.of(parseCodeFence(lines));
        }
        MediaSingle media = parseMedia(lines);
        if (media != null) {
            return List.of(media);
        }
        Table table = parseTable(lines);
        if (table != null) {
            return List.of(table);
        }
        List<AdfNode> list = parseList(lines);
        if (list != null) {
            return list;
        }
        return List.of(new Paragraph(mapLinesToInline(lines)));
    }

    private MediaSingle parseMedia(List<String> lines) {
        if (lines.size() != 1) {
            return null;
        }
        Matcher matcher = MEDIA_PATTERN.matcher(lines.get(0).trim());
        if (!matcher.matches()) {
            return null;
        }
        Media media = MediaUtil.fromImageSrc("media:" + matcher.group(1));
        return new MediaSingle(List.of(media));
    }

    private boolean isCodeFence(List<String> lines) {
        return !lines.isEmpty() && CODE_FENCE_PATTERN.matcher(lines.get(0)).matches();
    }

    private AdfNode parseCodeFence(List<String> lines) {
        Matcher m = CODE_FENCE_PATTERN.matcher(lines.get(0));
        String language = "";
        if (m.matches()) {
            language = m.group(1).trim();
        }
        StringBuilder body = new StringBuilder();
        int end = lines.size();
        if (end > 1 && lines.get(end - 1).startsWith("```")) {
            end--;
        }
        for (int i = 1; i < end; i++) {
            String line = lines.get(i);
            body.append(line);
            if (i < end - 1) {
                body.append("\n");
            }
        }
        return new CodeBlock(language.isBlank() ? null : language, body.toString());
    }

    private Table parseTable(List<String> lines) {
        List<AdfNode> rows = new ArrayList<>();
        for (String line : lines) {
            List<String> cells = parseTableLine(line);
            if (cells == null) {
                return null;
            }
            if (isDelimiterRow(cells)) {
                continue;
            }
            boolean header = rows.isEmpty();
            List<AdfNode> rowCells = new ArrayList<>();
            for (String cell : cells) {
                List<AdfNode> content = List.of(new Paragraph(List.of(new Text(cell))));
                rowCells.add(header ? new TableHeader(content, null) : new TableCell(content, null));
            }
            rows.add(new TableRow(rowCells));
        }
        return rows.isEmpty() ? null : new Table(rows);
    }

    private List<String> parseTableLine(String line) {
        if (line == null || !line.contains("|")) {
            return null;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) {
            return null;
        }
        String inner = trimmed.substring(1, trimmed.length() - 1);
        String[] parts = inner.split("\\|", -1);
        List<String> cells = new ArrayList<>();
        for (String part : parts) {
            cells.add(part.trim());
        }
        return cells;
    }

    private boolean isDelimiterRow(List<String> cells) {
        return !cells.isEmpty() && cells.stream().allMatch(cell -> cell.matches(":?-{3,}:?"));
    }

    private List<AdfNode> parseList(List<String> lines) {
        List<ListItemLine> items = new ArrayList<>();
        for (String line : lines) {
            Matcher task = TASK_PATTERN.matcher(line);
            Matcher ordered = ORDERED_PATTERN.matcher(line);
            Matcher bullet = BULLET_PATTERN.matcher(line);
            if (task.matches()) {
                items.add(new ListItemLine(LineKind.TASK, task.group(1).length(), task.group(3), "x".equalsIgnoreCase(task.group(2).trim())));
            } else if (ordered.matches()) {
                items.add(new ListItemLine(LineKind.ORDERED, ordered.group(1).length(), ordered.group(3), false));
            } else if (bullet.matches()) {
                items.add(new ListItemLine(LineKind.BULLET, bullet.group(1).length(), bullet.group(3), false));
            } else {
                items.clear();
                break;
            }
        }

        if (items.isEmpty()) {
            return null;
        }

        List<AdfNode> lists = new ArrayList<>();
        int index = 0;
        while (index < items.size()) {
            LineKind kind = items.get(index).kind;
            List<ListItemLine> group = new ArrayList<>();
            while (index < items.size() && items.get(index).kind == kind) {
                group.add(items.get(index));
                index++;
            }
            lists.add(toList(kind, group));
        }
        return lists;
    }

    private AdfNode toList(LineKind kind, List<ListItemLine> items) {
        if (kind == LineKind.TASK) {
            List<AdfNode> taskItems = new ArrayList<>();
            for (ListItemLine item : items) {
                String state = item.done ? "DONE" : "TODO";
                taskItems.add(new TaskItem(null, state, List.of(new Paragraph(mapLinesToInline(List.of(item.text))))));
            }
            return new TaskList(taskItems);
        }
        List<AdfNode> listItems = new ArrayList<>();
        for (ListItemLine item : items) {
            listItems.add(new com.tsystems.jira.adf.model.ListItem(List.of(new Paragraph(mapLinesToInline(List.of(item.text))))));
        }
        return kind == LineKind.ORDERED ? new OrderedList(listItems) : new BulletList(listItems);
    }

    private List<AdfNode> mapLinesToInline(List<String> lines) {
        List<AdfNode> inlines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                inlines.add(new HardBreak());
            }
            inlines.add(new Text(lines.get(i)));
        }
        return inlines.isEmpty() ? List.of(new Text("")) : inlines;
    }

    private enum LineKind { BULLET, ORDERED, TASK }

    private static class ListItemLine {
        final LineKind kind;
        final int indent;
        final String text;
        final boolean done;

        ListItemLine(LineKind kind, int indent, String text, boolean done) {
            this.kind = kind;
            this.indent = indent;
            this.text = text;
            this.done = done;
        }
    }
}
