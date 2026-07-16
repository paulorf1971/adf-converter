package com.tsystems.jira.adf.markdown;

import com.tsystems.jira.adf.api.InboundConverter;
import com.tsystems.jira.adf.api.OutboundConverter;
import com.tsystems.jira.adf.config.ConverterConfig;

/**
 * Factory facade for Markdown converters.
 */
public final class MarkdownConverters {

    private MarkdownConverters() {
    }

    public static OutboundConverter<String> outbound(ConverterConfig config) {
        return new AdfToMarkdownConverter(config);
    }

    public static InboundConverter<String> inbound(ConverterConfig config) {
        return new MarkdownToAdfConverter(config);
    }
}

