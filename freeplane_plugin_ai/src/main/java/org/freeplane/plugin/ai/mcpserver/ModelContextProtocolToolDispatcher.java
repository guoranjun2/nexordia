package org.freeplane.plugin.ai.mcpserver;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutionResult;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ModelContextProtocolToolDispatcher {
    private final Object toolSet;
    private final ObjectMapper objectMapper;
    private final Map<String, Method> toolMethods;

    public ModelContextProtocolToolDispatcher(Object toolSet, ObjectMapper objectMapper) {
        this.toolSet = Objects.requireNonNull(toolSet, "toolSet");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.toolMethods = buildToolMethods(toolSet);
    }

    public ToolExecutionResult dispatch(String toolName, JsonNode argumentsNode) {
        Method method = toolMethods.get(toolName);
        if (method == null) {
            throw new IllegalArgumentException("Unknown tool name: " + toolName);
        }
        String arguments = "{}";
        if (argumentsNode != null && !argumentsNode.isNull()) {
            try {
                arguments = objectMapper.writeValueAsString(argumentsNode);
            } catch (Exception error) {
                throw new IllegalArgumentException("Invalid tool arguments.", error);
            }
        }
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name(toolName)
            .arguments(arguments)
            .build();
        DefaultToolExecutor executor = new DefaultToolExecutor(toolSet, method);
        return executor.executeWithContext(request, InvocationContext.builder().build());
    }

    private static Map<String, Method> buildToolMethods(Object toolSet) {
        Map<String, Method> methods = new LinkedHashMap<>();
        List<ToolSpecification> specifications = ToolSpecifications.toolSpecificationsFrom(toolSet);
        for (Method method : toolSet.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class)) {
                continue;
            }
            ToolSpecification specification = ToolSpecifications.toolSpecificationFrom(method);
            methods.put(specification.name(), method);
        }
        ToolSpecifications.validateSpecifications(specifications);
        return methods;
    }
}
