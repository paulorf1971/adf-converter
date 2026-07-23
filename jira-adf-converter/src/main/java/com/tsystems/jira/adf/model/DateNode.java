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
public class DateNode extends AdfNode {

	@Builder
	public DateNode(@JsonProperty("attrs") Map<String, Object> attrs) {
		super("date", null, attrs);
	}

	public DateNode(String timestamp) {
		super("date", null, withTimestamp(timestamp));
	}

	private static Map<String, Object> withTimestamp(String timestamp) {
		Map<String, Object> map = new HashMap<>();
		map.put("timestamp", timestamp);
		return map;
	}
}

