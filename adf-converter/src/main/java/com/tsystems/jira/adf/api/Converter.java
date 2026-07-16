package com.tsystems.jira.adf.api;

public interface Converter<I, O> {

	O convert(I input, ConverterContext context) throws ConversionException;
}
