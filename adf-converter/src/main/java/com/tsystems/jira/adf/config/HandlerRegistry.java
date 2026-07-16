package com.tsystems.jira.adf.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.tsystems.jira.adf.registry.MarkHandler;
import com.tsystems.jira.adf.registry.NodeHandler;

public class HandlerRegistry {

	private final Map<String, NodeHandler<?, ?>> nodeHandlers = new HashMap<>();
	private final Map<String, MarkHandler<?, ?>> markHandlers = new HashMap<>();

	public <I, O> void registerNodeHandler(String type, NodeHandler<I, O> handler) {
		nodeHandlers.put(type, handler);
	}

	public <I, O> void registerMarkHandler(String type, MarkHandler<I, O> handler) {
		markHandlers.put(type, handler);
	}

	@SuppressWarnings("unchecked")
	public <I, O> Optional<NodeHandler<I, O>> getNodeHandler(String type) {
		return Optional.ofNullable((NodeHandler<I, O>) nodeHandlers.get(type));
	}

	@SuppressWarnings("unchecked")
	public <I, O> Optional<MarkHandler<I, O>> getMarkHandler(String type) {
		return Optional.ofNullable((MarkHandler<I, O>) markHandlers.get(type));
	}
}

