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
public class TaskItem extends AdfNode {

	@Builder
	public TaskItem(@JsonProperty("content") List<AdfNode> content,
				   @JsonProperty("attrs") Map<String, Object> attrs) {
		super("taskItem", content, attrs);
	}

	public TaskItem(String localId, String state, List<AdfNode> content) {
		super("taskItem", content, withAttrs(localId, state));
	}

	private static Map<String, Object> withAttrs(String localId, String state) {
		Map<String, Object> map = new HashMap<>();
		if (localId != null) {
			map.put("localId", localId);
		}
		if (state != null) {
			map.put("state", state);
		}
		return map;
	}
}

