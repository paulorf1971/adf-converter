package com.tsystems.jira.adf.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.model.Document;
import com.tsystems.jira.adf.model.Emoji;
import com.tsystems.jira.adf.model.AdfNode;
import com.tsystems.jira.adf.model.Paragraph;
import com.tsystems.jira.adf.model.Text;

class JsonUtilTest {

	@Test
	void shouldBuildConfigAndSerializeDocument() throws Exception {
		ConverterConfig config = ConverterConfig.builder()
				.allowUnknownMarks(true)
				.escapeHtml(false)
				.mediaBaseUrl("https://example.com")
				.build();

		assertTrue(config.isAllowUnknownMarks());
		assertFalse(config.isEscapeHtml());
		assertTrue(config.getMediaBaseUrl().contains("example.com"));

		Document document = new Document(List.of(new Paragraph(List.of(new Text("Hello ADF", null)))));
		String json = JsonUtil.toJson(document);

		assertNotNull(json);
		JsonNode node = JsonUtil.mapper().readTree(json);
		assertTrue(node.has("type"));
		assertTrue(node.get("content").isArray());
	}

	@Test
	void shouldDeserializePolymorphicDocumentNodes() {
		String json = "{\"type\":\"doc\",\"version\":1,\"content\":[" +
				"{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}," +
				"{\"type\":\"emoji\",\"attrs\":{\"shortName\":\":smile:\",\"text\":\"\\uD83D\\uDE0A\"}}]}";

		Document doc = JsonUtil.fromJson(json, Document.class);

		assertTrue(doc.getContent().get(0) instanceof Paragraph);
		assertTrue(((Paragraph) doc.getContent().get(0)).getContent().get(0) instanceof Text);
		assertTrue(doc.getContent().get(1) instanceof Emoji);
	}

	@Test
	void shouldDeserializeEveryRegisteredAdfNodeSubtype() {
		String json = "{\"type\":\"doc\",\"version\":1,\"content\":[" +
				"{\"type\":\"heading\",\"attrs\":{\"level\":1},\"content\":[{\"type\":\"text\",\"text\":\"H\"}]}," +
				"{\"type\":\"paragraph\",\"content\":[{\"type\":\"hardBreak\"}]}," +
				"{\"type\":\"blockquote\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"q\"}]}]}," +
				"{\"type\":\"bulletList\",\"content\":[{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"b\"}]}]}]}," +
				"{\"type\":\"orderedList\",\"attrs\":{\"order\":2},\"content\":[{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"o\"}]}]}]}," +
				"{\"type\":\"codeBlock\",\"attrs\":{\"language\":\"java\"},\"text\":\"code\"}," +
				"{\"type\":\"panel\",\"attrs\":{\"panelType\":\"info\"},\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"p\"}]}]}," +
				"{\"type\":\"status\",\"attrs\":{\"text\":\"ok\",\"color\":\"green\"}}," +
				"{\"type\":\"taskList\",\"content\":[{\"type\":\"taskItem\",\"attrs\":{\"state\":\"DONE\"},\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"t\"}]}]}]}," +
				"{\"type\":\"decisionList\",\"content\":[{\"type\":\"decisionItem\",\"attrs\":{\"localId\":\"d1\"},\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"d\"}]}]}]}," +
				"{\"type\":\"table\",\"content\":[{\"type\":\"tableRow\",\"content\":[{\"type\":\"tableHeader\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"h\"}]}]},{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"c\"}]}]}]}]}," +
				"{\"type\":\"mediaSingle\",\"content\":[{\"type\":\"media\",\"attrs\":{\"id\":\"m1\",\"type\":\"file\"}}]}," +
				"{\"type\":\"mediaGroup\",\"content\":[{\"type\":\"media\",\"attrs\":{\"id\":\"m2\",\"type\":\"file\"}}]}," +
				"{\"type\":\"paragraph\",\"content\":[{\"type\":\"mediaInline\",\"content\":[{\"type\":\"media\",\"attrs\":{\"id\":\"m3\",\"type\":\"file\"}}]},{\"type\":\"emoji\",\"attrs\":{\"shortName\":\":smile:\"}},{\"type\":\"date\",\"attrs\":{\"timestamp\":\"1700000000000\"}}]}" +
				"]}";

		Document doc = JsonUtil.fromJson(json, Document.class);

		assertTrue(doc.getContent().stream().allMatch(AdfNode.class::isInstance));
		assertTrue(doc.getContent().size() >= 14);
	}
}
