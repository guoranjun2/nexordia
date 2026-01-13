package org.freeplane.plugin.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ToolExecutorFactory {
    private final boolean wrapToolArgumentsExceptions;
    private final boolean propagateToolExecutionExceptions;

    public ToolExecutorFactory(boolean wrapToolArgumentsExceptions, boolean propagateToolExecutionExceptions) {
        this.wrapToolArgumentsExceptions = wrapToolArgumentsExceptions;
        this.propagateToolExecutionExceptions = propagateToolExecutionExceptions;
    }

    public ToolExecutorRegistry createRegistry(Object toolSet) {
        Objects.requireNonNull(toolSet, "toolSet");
        Map<String, ToolExecutor> executorsByName = new LinkedHashMap<>();
        Map<ToolSpecification, ToolExecutor> executorsBySpecification = new LinkedHashMap<>();
        List<ToolSpecification> specifications = new ArrayList<>();
        for (Method method : toolSet.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Tool.class)) {
                continue;
            }
            ToolSpecification specification = ToolSpecifications.toolSpecificationFrom(method);
            DefaultToolExecutor executor = DefaultToolExecutor.builder()
                .object(toolSet)
                .originalMethod(method)
                .methodToInvoke(method)
                .wrapToolArgumentsExceptions(wrapToolArgumentsExceptions)
                .propagateToolExecutionExceptions(propagateToolExecutionExceptions)
                .build();
            ToolExecutor toolExecutor = new EventDispatchToolExecutor(executor);
            executorsByName.put(specification.name(), toolExecutor);
            executorsBySpecification.put(specification, toolExecutor);
            specifications.add(specification);
        }
        ToolSpecifications.validateSpecifications(specifications);
        return new ToolExecutorRegistry(
            Collections.unmodifiableMap(executorsByName),
            Collections.unmodifiableMap(executorsBySpecification));
    }
}
