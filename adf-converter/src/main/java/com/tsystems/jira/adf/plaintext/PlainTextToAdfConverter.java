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
import com.tsystems.jira.adf.model.OrderedList;
import com.tsystems.jira.adf.model.Paragraph;
import com.tsystems.jira.adf.model.TaskItem;
import com.tsystems.jira.adf.model.TaskList;
import com.tsystems.jira.adf.model.Text;

/**
 * Minimal Plain Text → ADF parser.
 */
public class PlainTextToAdfConverter implements InboundConverter<String> {

    private static final Pattern ORDERED_PATTERN = Pattern.compile("^(\\s*)(\\d+)\\. (.*)$");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^(\\s*)([-*]) (.*)$");
    private static final Pattern TASK_PATTERN = Pattern.compile("^(\\s*)- \\[( |x|X)\\] (.*)$");
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("^```(.*)$");

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
        String[] paragraphs = input.split("\\n\\n");
        for (String para : paragraphs) {
            if (para.isBlank()) {
                continue;
            }
            blocks.add(parseParagraph(para));
        }
        return new Document(1, blocks);
    }

    private AdfNode parseParagraph(String para) {
        List<String> lines = List.of(para.split("\\n", -1));
        if (isCodeFence(lines)) {
            return parseCodeFence(lines);
        }
        List<AdfNode> list = parseList(lines);
        if (list != null) {
            return list.get(0);
        }
        return new Paragraph(mapLinesToInline(lines));
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
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("```") && i == lines.size() - 1) {
                break;
            }
            body.append(line);
            if (i < lines.size() - 1) {
                body.append("\n");
            }
        }
        return new CodeBlock(language.isBlank() ? null : language, body.toString());
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

        // Minimal: single-level lists based on first item kind.
        LineKind kind = items.get(0).kind;
        if (kind == LineKind.TASK) {
            List<AdfNode> taskItems = new ArrayList<>();
            for (ListItemLine item : items) {
                String state = item.done ? "DONE" : "TODO";
                taskItems.add(new TaskItem(null, state, List.of(new Paragraph(mapLinesToInline(List.of(item.text))))));
            }
            return List.of(new TaskList(taskItems));
        }
        if (kind == LineKind.ORDERED) {
            List<AdfNode> li = new ArrayList<>();
            for (ListItemLine item : items) {
                li.add(new com.tsystems.jira.adf.model.ListItem(List.of(new Paragraph(mapLinesToInline(List.of(item.text))))));
            }
            return List.of(new OrderedList(li));
        }
        if (kind == LineKind.BULLET) {
            List<AdfNode> li = new ArrayList<>();
            for (ListItemLine item : items) {
                li.add(new com.tsystems.jira.adf.model.ListItem(List.of(new Paragraph(mapLinesToInline(List.of(item.text))))));
            }
            return List.of(new BulletList(li));
        }
        return null;
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
