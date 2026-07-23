package com.tsystems.jira.adf.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.tsystems.jira.adf.model.AdfNode;
import com.tsystems.jira.adf.model.Table;
import com.tsystems.jira.adf.model.TableCell;
import com.tsystems.jira.adf.model.TableCellBase;
import com.tsystems.jira.adf.model.TableHeader;
import com.tsystems.jira.adf.model.TableRow;

/**
 * Utility helpers for table normalization and rendering defaults.
 */
public final class TableUtil {

    private TableUtil() {
    }

    public static Table normalize(Table table) {
        if (table == null) return null;
        List<AdfNode> rows = table.getContent();
        List<AdfNode> normalizedRows = new ArrayList<>();
        if (rows != null) {
            for (AdfNode rowNode : rows) {
                if (!(rowNode instanceof TableRow row)) continue;
                List<AdfNode> cells = row.getContent();
                List<AdfNode> normalizedCells = new ArrayList<>();
                if (cells != null) {
                    for (AdfNode cellNode : cells) {
                        if (cellNode instanceof TableCellBase cell) {
                            normalizedCells.add(normalizeCell(cell));
                        }
                    }
                }
                normalizedRows.add(new TableRow(normalizedCells));
            }
        }
        return new Table(normalizedRows);
    }

    private static TableCellBase normalizeCell(TableCellBase cell) {
        Map<String, Object> attrs = cell.getAttrs();
        Integer colspan = asInt(attrs, "colspan", 1);
        Integer rowspan = asInt(attrs, "rowspan", 1);
        String background = attrs == null ? null : (String) attrs.get("background");
        String align = attrs == null ? null : (String) attrs.get("align");
        List<Integer> colwidth = attrs == null ? null : (List<Integer>) attrs.get("colwidth");
        if (cell instanceof TableHeader th) {
            return new TableHeader(copyContent(cell), colspan, rowspan, background, align, colwidth);
        }
        return new TableCell(copyContent(cell), colspan, rowspan, background, align, colwidth);
    }

    private static List<AdfNode> copyContent(TableCellBase cell) {
        if (cell.getContent() == null) return List.of();
        return new ArrayList<>(cell.getContent());
    }

    @SuppressWarnings("unchecked")
    private static Integer asInt(Map<String, Object> attrs, String key, int def) {
        Object v = attrs == null ? null : attrs.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    public static String htmlAlign(Map<String, Object> attrs) {
        String align = attrs == null ? null : Objects.toString(attrs.get("align"), null);
        return align == null ? null : align;
    }

    public static String backgroundStyle(Map<String, Object> attrs) {
        String bg = attrs == null ? null : Objects.toString(attrs.get("background"), null);
        return bg == null || bg.isBlank() ? null : "background:" + bg + ";";
    }
}

