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
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderedList extends AdfNode {

	@Builder
	public OrderedList(@JsonProperty("content") List<AdfNode> content,
					  @JsonProperty("attrs") Map<String, Object> attrs) {
		super("orderedList", content, attrs);
	}

	public OrderedList(List<AdfNode> content) {
		super("orderedList", content, null);
	}

	public OrderedList(int order, List<AdfNode> content) {
		super("orderedList", content, withOrder(order));
	}

	private static Map<String, Object> withOrder(int order) {
		Map<String, Object> map = new HashMap<>();
		map.put("order", order);
		return map;
	}
}
