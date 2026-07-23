package com.tsystems.jira.adf.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Document extends AdfNode {

	@JsonProperty("version")
	private int version = 1;

	public Document(List<AdfNode> content) {
		super("doc", content);
	}

	public Document(int version, List<AdfNode> content) {
		super("doc", content);
		this.version = version;
	}
}
