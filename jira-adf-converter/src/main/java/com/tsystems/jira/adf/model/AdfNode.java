package com.tsystems.jira.adf.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
		@JsonSubTypes.Type(value = Document.class, name = "doc"),
		@JsonSubTypes.Type(value = Paragraph.class, name = "paragraph"),
		@JsonSubTypes.Type(value = Heading.class, name = "heading"),
		@JsonSubTypes.Type(value = Text.class, name = "text"),
		@JsonSubTypes.Type(value = HardBreak.class, name = "hardBreak"),
		@JsonSubTypes.Type(value = Blockquote.class, name = "blockquote"),
		@JsonSubTypes.Type(value = BulletList.class, name = "bulletList"),
		@JsonSubTypes.Type(value = OrderedList.class, name = "orderedList"),
		@JsonSubTypes.Type(value = ListItem.class, name = "listItem"),
		@JsonSubTypes.Type(value = CodeBlock.class, name = "codeBlock"),
		@JsonSubTypes.Type(value = Panel.class, name = "panel"),
		@JsonSubTypes.Type(value = Status.class, name = "status"),
		@JsonSubTypes.Type(value = TaskList.class, name = "taskList"),
		@JsonSubTypes.Type(value = TaskItem.class, name = "taskItem"),
		@JsonSubTypes.Type(value = DecisionList.class, name = "decisionList"),
		@JsonSubTypes.Type(value = DecisionItem.class, name = "decisionItem"),
		@JsonSubTypes.Type(value = Table.class, name = "table"),
		@JsonSubTypes.Type(value = TableRow.class, name = "tableRow"),
		@JsonSubTypes.Type(value = TableCell.class, name = "tableCell"),
		@JsonSubTypes.Type(value = TableHeader.class, name = "tableHeader"),
		@JsonSubTypes.Type(value = Media.class, name = "media"),
		@JsonSubTypes.Type(value = MediaSingle.class, name = "mediaSingle"),
		@JsonSubTypes.Type(value = MediaGroup.class, name = "mediaGroup"),
		@JsonSubTypes.Type(value = MediaInline.class, name = "mediaInline"),
		@JsonSubTypes.Type(value = Emoji.class, name = "emoji"),
		@JsonSubTypes.Type(value = DateNode.class, name = "date")
})
public abstract class AdfNode {

	protected AdfNode() {
	}

	@JsonProperty("type")
	private String type;

	@JsonProperty("content")
	private List<AdfNode> content = new ArrayList<>();

	@JsonProperty("attrs")
	private Map<String, Object> attrs = new HashMap<>();

	protected AdfNode(String type) {
		this.type = type;
	}

	protected AdfNode(String type, List<AdfNode> content) {
		this.type = type;
		if (content != null) {
			this.content = content;
		}
	}

	protected AdfNode(String type, List<AdfNode> content, Map<String, Object> attrs) {
		this.type = type;
		if (content != null) {
			this.content = content;
		}
		if (attrs != null) {
			this.attrs = attrs;
		}
	}
}
