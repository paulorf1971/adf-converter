package com.tsystems.jira.adf.plaintext;

import com.tsystems.jira.adf.api.InboundConverter;
import com.tsystems.jira.adf.api.OutboundConverter;
import com.tsystems.jira.adf.config.ConverterConfig;

public final class PlainTextConverters {

    private PlainTextConverters() {
    }

    public static OutboundConverter<String> outbound(ConverterConfig config) {
        return new AdfToPlainTextConverter(config);
    }

    public static InboundConverter<String> inbound(ConverterConfig config) {
        return new PlainTextToAdfConverter(config);
    }
}

