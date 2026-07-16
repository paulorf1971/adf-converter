package com.tsystems.jira.adf.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.model.Document;
import com.tsystems.jira.adf.model.Paragraph;
import com.tsystems.jira.adf.model.Text;

class JsonUtilTest {

	@Test
	void shouldBuildConfigAndSerializeDocument() {
		ConverterConfig config = ConverterConfig.builder()
				.allowUnknownMarks(true)
				.escapeHtml(false)
				.mediaBaseUrl("https://example.com")
				.textColor("red", "#ff0000")
				.build();

		assertTrue(config.isAllowUnknownMarks());
		assertFalse(config.isEscapeHtml());
		assertTrue(config.getTextColorPalette().containsKey("red"));

		Document document = new Document(List.of(new Paragraph(List.of(new Text("Hello ADF", null)))));
		String json = JsonUtil.toJson(document);

		assertNotNull(json);
		JsonNode node = JsonUtil.mapper().readTree(json);
		assertTrue(node.has("type"));
		assertTrue(node.get("content").isArray());
	}
}

