package com.tsystems.jira.adf.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ConverterConfig {

	@Builder.Default
	private boolean allowUnknownMarks = false;

    @Builder.Default
    private boolean escapeHtml = true;

    @Builder.Default
    private String mediaBaseUrl = "";

    @Builder.Default
    private boolean mediaPlaceholder = true;
}
