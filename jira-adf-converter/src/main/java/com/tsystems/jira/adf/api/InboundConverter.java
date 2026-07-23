package com.tsystems.jira.adf.api;

import com.tsystems.jira.adf.model.Document;

public interface InboundConverter<S> extends Converter<S, Document> {

	@Override
	Document convert(S input, ConverterContext context) throws ConversionException;
}
