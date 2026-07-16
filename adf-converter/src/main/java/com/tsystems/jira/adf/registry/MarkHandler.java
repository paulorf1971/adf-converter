package com.tsystems.jira.adf.registry;

import com.tsystems.jira.adf.api.ConverterContext;
import com.tsystems.jira.adf.config.ConverterConfig;
import com.tsystems.jira.adf.config.HandlerRegistry;
import com.tsystems.jira.adf.model.Mark;

public interface MarkHandler<I, O> {

	O handle(I node, Mark mark, ConverterContext context, ConverterConfig config, HandlerRegistry registry);
}

