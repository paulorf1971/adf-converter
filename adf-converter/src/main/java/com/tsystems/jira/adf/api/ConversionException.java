package com.tsystems.jira.adf.api;

public class ConversionException extends RuntimeException {

	private static final long serialVersionUID = 6953818079336505567L;

	public ConversionException(String message) {
		super(message);
	}

	public ConversionException(String message, Throwable cause) {
		super(message, cause);
	}
}

