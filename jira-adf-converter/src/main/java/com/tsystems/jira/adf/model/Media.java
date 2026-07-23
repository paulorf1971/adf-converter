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
public class Media extends AdfNode {

	@Builder
	public Media(@JsonProperty("attrs") Map<String, Object> attrs) {
		super("media", null, attrs);
	}

	public Media(String id, String type, String collection, Integer width, Integer height) {
		super("media", null, withAttrs(id, type, collection, width, height));
	}

	private static Map<String, Object> withAttrs(String id, String type, String collection, Integer width,
										 Integer height) {
		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		map.put("type", type);
		if (collection != null) {
			map.put("collection", collection);
		}
		if (width != null) {
			map.put("width", width);
		}
		if (height != null) {
			map.put("height", height);
		}
		return map;
	}
}

