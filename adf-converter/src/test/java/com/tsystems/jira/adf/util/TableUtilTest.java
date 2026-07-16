package com.tsystems.jira.adf.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tsystems.jira.adf.model.Document;
import com.tsystems.jira.adf.model.Paragraph;
import com.tsystems.jira.adf.model.Table;
import com.tsystems.jira.adf.model.TableCell;
import com.tsystems.jira.adf.model.TableHeader;
import com.tsystems.jira.adf.model.TableRow;
import com.tsystems.jira.adf.model.Text;

class TableUtilTest {

    @Test
    void normalize_fills_missing_rowcell_defaults() {
        Table table = new Table(List.of(
                new TableRow(List.of(
                        new TableHeader(List.of(new Text("H1")), null),
                        new TableCell(List.of(new Text("C1")), null)
                ))
        ));

        Table normalized = TableUtil.normalize(table);

        TableHeader th = (TableHeader) normalized.getContent().get(0).getContent().get(0);
        TableCell td = (TableCell) normalized.getContent().get(0).getContent().get(1);
        assertThat(th.getAttrs().get("colspan")).isEqualTo(1);
        assertThat(th.getAttrs().get("rowspan")).isEqualTo(1);
        assertThat(td.getAttrs().get("colspan")).isEqualTo(1);
        assertThat(td.getAttrs().get("rowspan")).isEqualTo(1);
    }

    @Test
    void html_background_and_align_helpers() {
        TableCell cell = new TableCell(List.of(new Paragraph(List.of(new Text("x")))), 2, 3, "#fff", "center", List.of());

        assertThat(TableUtil.htmlAlign(cell.getAttrs())).isEqualTo("center");
        assertThat(TableUtil.backgroundStyle(cell.getAttrs())).isEqualTo("background:#fff;");
    }
}

