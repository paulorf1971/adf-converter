package com.tsystems.jira.adf.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Panel extends AdfNode {

	@Builder
	public Panel(@JsonProperty("content") List<AdfNode> content,
				@JsonProperty("attrs") Map<String, Object> attrs) {
		super("panel", content, attrs);
	}

	public Panel(String panelType, List<AdfNode> content) {
		super("panel", content, withPanelType(panelType));
	}

	private static Map<String, Object> withPanelType(String panelType) {
		Map<String, Object> map = new HashMap<>();
		map.put("panelType", panelType);
		return map;
	}
}

