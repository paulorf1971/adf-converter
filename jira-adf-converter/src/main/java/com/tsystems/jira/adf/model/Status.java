package com.tsystems.jira.adf.model;

import java.util.HashMap;
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
public class Status extends AdfNode {

	@Builder
	public Status(@JsonProperty("attrs") Map<String, Object> attrs) {
		super("status", null, attrs);
	}

	public Status(String text, String color, String style) {
		super("status", null, withAttrs(text, color, style));
	}

	private static Map<String, Object> withAttrs(String text, String color, String style) {
		Map<String, Object> map = new HashMap<>();
		map.put("text", text);
		if (color != null) {
			map.put("color", color);
		}
		if (style != null) {
			map.put("style", style);
		}
		return map;
	}
}

