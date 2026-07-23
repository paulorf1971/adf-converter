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
public class CodeBlock extends AdfNode {

	@JsonProperty("text")
	private String text;

	@Builder
	public CodeBlock(@JsonProperty("text") String text,
				   @JsonProperty("attrs") Map<String, Object> attrs) {
		super("codeBlock", null, attrs);
		this.text = text;
	}

	public CodeBlock(String language, String text) {
		super("codeBlock", null, withLanguage(language));
		this.text = text;
	}

	public CodeBlock(List<AdfNode> content, Map<String, Object> attrs) {
		super("codeBlock", content, attrs);
	}

	private static Map<String, Object> withLanguage(String language) {
		Map<String, Object> map = new HashMap<>();
		map.put("language", language);
		return map;
	}
}

