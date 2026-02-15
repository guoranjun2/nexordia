package org.freeplane.plugin.ai.tools.utilities;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.Map;

public class ToolExecutorRegistry {
    private final Map<String, ToolExecutor> executorsByName;
    private final Map<ToolSpecification, ToolExecutor> executorsBySpecification;

    ToolExecutorRegistry(Map<String, ToolExecutor> executorsByName,
                         Map<ToolSpecification, ToolExecutor> executorsBySpecification) {
        this.executorsByName = executorsByName;
        this.executorsBySpecification = executorsBySpecification;
    }

    public Map<String, ToolExecutor> getExecutorsByName() {
        return executorsByName;
    }

    public Map<ToolSpecification, ToolExecutor> getExecutorsBySpecification() {
        return executorsBySpecification;
    }
}
