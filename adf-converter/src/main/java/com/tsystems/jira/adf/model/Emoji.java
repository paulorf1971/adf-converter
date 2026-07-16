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
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Emoji extends AdfNode {

	@Builder
	public Emoji(@JsonProperty("attrs") Map<String, Object> attrs) {
		super("emoji", null, attrs);
	}

	public Emoji(String shortName, String id, String text) {
		super("emoji", null, withAttrs(shortName, id, text));
	}

	private static Map<String, Object> withAttrs(String shortName, String id, String text) {
		Map<String, Object> map = new HashMap<>();
		map.put("shortName", shortName);
		if (id != null) {
			map.put("id", id);
		}
		if (text != null) {
			map.put("text", text);
		}
		return map;
	}
}
