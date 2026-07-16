package com.tsystems.jira.adf.registry;

import com.tsystems.jira.adf.api.ConverterContext;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.config.HandlerRegistry;

public interface NodeHandler<I, O> {

	O handle(I node, ConverterContext context, ConverterConfig config, HandlerRegistry registry);
}

