package com.tsystems.jira.adf.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Mark {

	@JsonProperty("type")
	private String type;

	@Builder.Default
	@JsonProperty("attrs")
	private Map<String, Object> attrs = new HashMap<>();

	public static Mark code() {
		return Mark.builder().type("code").build();
	}

	public static Mark em() {
		return Mark.builder().type("em").build();
	}

	public static Mark strong() {
		return Mark.builder().type("strong").build();
	}

	public static Mark strike() {
		return Mark.builder().type("strike").build();
	}

	public static Mark underline() {
		return Mark.builder().type("underline").build();
	}

	public static Mark link(String href, String title) {
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("href", href);
		if (title != null) {
			attrs.put("title", title);
		}
		return Mark.builder().type("link").attrs(attrs).build();
	}

	public static Mark subSup(String type) {
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("type", type);
		return Mark.builder().type("subsup").attrs(attrs).build();
	}

	public static Mark textColor(String color) {
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("color", color);
		return Mark.builder().type("textColor").attrs(attrs).build();
	}
}

