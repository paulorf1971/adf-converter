package com.tsystems.jira.adf.model;

import java.util.ArrayList;
import java.util.List;

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
public class Text extends AdfNode {

	@JsonProperty("text")
	private String text;

	@JsonProperty("marks")
	private List<Mark> marks = new ArrayList<>();

	public Text(String text) {
		this(text, new ArrayList<>());
	}

	@Builder
	public Text(@JsonProperty("text") String text,
		   @JsonProperty("marks") List<Mark> marks) {
		super("text", null, null);
		this.text = text;
		if (marks != null) {
			this.marks = marks;
		}
	}
}
