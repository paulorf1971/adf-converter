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
public class Heading extends AdfNode {

	@Builder
	public Heading(@JsonProperty("content") List<AdfNode> content,
				  @JsonProperty("attrs") Map<String, Object> attrs) {
		super("heading", content, attrs);
	}

	public Heading(int level, List<AdfNode> content) {
		super("heading", content, withLevel(level));
	}

	private static Map<String, Object> withLevel(int level) {
		Map<String, Object> map = new HashMap<>();
		map.put("level", level);
		return map;
	}
}

