package com.tsystems.jira.adf.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TableCell extends TableCellBase {

	@Builder
	public TableCell(List<AdfNode> content, Map<String, Object> attrs) {
		super("tableCell", content, attrs);
	}

	public TableCell(List<AdfNode> content, Integer colspan, Integer rowspan, String background, String align,
				  List<Integer> colwidth) {
		super("tableCell", content, colspan, rowspan, background, align, colwidth);
	}
}

