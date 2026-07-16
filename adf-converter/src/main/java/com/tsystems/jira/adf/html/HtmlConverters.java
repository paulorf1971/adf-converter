package com.tsystems.jira.adf.html;

import com.tsystems.jira.adf.api.InboundConverter;
import com.tsystems.jira.adf.api.OutboundConverter;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.model.Document;

/**
 * Factory helpers for HTML converters.
 */
public final class HtmlConverters {

    private HtmlConverters() {
    }

    public static OutboundConverter<String> outbound(ConverterConfig config) {
        return new AdfToHtmlConverter(config);
    }

    public static InboundConverter<String> inbound(ConverterConfig config) {
        return new HtmlToAdfConverter(config);
    }
}

