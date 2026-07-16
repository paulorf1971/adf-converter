package com.tsystems.jira.adf.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TableCellBase extends AdfNode {

	protected TableCellBase(String type, List<AdfNode> content, Map<String, Object> attrs) {
		super(type, content, attrs);
	}

	protected TableCellBase(String type, List<AdfNode> content, Integer colspan, Integer rowspan,
						 String background, String align, List<Integer> colwidth) {
		super(type, content, buildAttrs(colspan, rowspan, background, align, colwidth));
	}

	private static Map<String, Object> buildAttrs(Integer colspan, Integer rowspan, String background, String align,
													List<Integer> colwidth) {
		Map<String, Object> map = new HashMap<>();
		if (colspan != null) {
			map.put("colspan", colspan);
		}
		if (rowspan != null) {
			map.put("rowspan", rowspan);
		}
		if (background != null) {
			map.put("background", background);
		}
		if (align != null) {
			map.put("align", align);
		}
		if (colwidth != null && !colwidth.isEmpty()) {
			map.put("colwidth", colwidth);
		}
		return map;
	}
}

