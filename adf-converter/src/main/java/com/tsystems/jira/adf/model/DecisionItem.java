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
public class DecisionItem extends AdfNode {

	@Builder
	public DecisionItem(@JsonProperty("content") List<AdfNode> content,
					  @JsonProperty("attrs") Map<String, Object> attrs) {
		super("decisionItem", content, attrs);
	}

	public DecisionItem(String localId, List<AdfNode> content) {
		super("decisionItem", content, withLocalId(localId));
	}

	private static Map<String, Object> withLocalId(String localId) {
		Map<String, Object> map = new HashMap<>();
		if (localId != null) {
			map.put("localId", localId);
		}
		return map;
	}
}
