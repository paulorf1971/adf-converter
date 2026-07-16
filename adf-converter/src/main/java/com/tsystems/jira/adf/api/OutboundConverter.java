package com.tsystems.jira.adf.api;

import com.tsystems.jira.adf.model.Document;

public interface OutboundConverter<T> extends Converter<Document, T> {

	@Override
	T convert(Document input, ConverterContext context) throws ConversionException;
}
