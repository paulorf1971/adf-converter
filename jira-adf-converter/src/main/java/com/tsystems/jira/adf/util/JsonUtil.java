package com.tsystems.jira.adf.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtil {

	private static final ObjectMapper MAPPER = createMapper();

	private JsonUtil() {
	}

	public static ObjectMapper mapper() {
		return MAPPER;
	}

	public static String toJson(Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize object to JSON", e);
		}
	}

	public static <T> T fromJson(String json, Class<T> type) {
		try {
			return MAPPER.readValue(json, type);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to deserialize JSON", e);
		}
	}

	private static ObjectMapper createMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper;
	}
}

