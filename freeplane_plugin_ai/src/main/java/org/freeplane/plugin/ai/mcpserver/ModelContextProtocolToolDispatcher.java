package org.freeplane.plugin.ai.mcpserver;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutionResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.freeplane.features.mode.Controller;
import org.freeplane.features.ui.ViewController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.freeplane.core.util.LogUtils;

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
        Controller controller = Controller.getCurrentController();
        if (controller == null) {
            throw new IllegalStateException("No current controller is available.");
        }
        ViewController viewController = controller.getViewController();
        if (viewController == null) {
            throw new IllegalStateException("No view controller is available.");
        }
        AtomicReference<ToolExecutionResult> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> executionError = new AtomicReference<>();
        Runnable runnable = () -> {
            try {
                resultRef.set(executeTool(toolName, argumentsNode));
            } catch (Throwable throwable) {
                executionError.set(throwable);
            }
        };
        try {
            viewController.invokeAndWait(runnable);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tool dispatch was interrupted.", interrupted);
        } catch (InvocationTargetException invocationTarget) {
            Throwable cause = invocationTarget.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Tool dispatch failed during invokeAndWait.", cause);
        }
        Throwable throwable = executionError.get();
        if (throwable != null) {
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Tool execution failed.", throwable);
        }
        ToolExecutionResult result = resultRef.get();
        if (result == null) {
            throw new IllegalStateException("Tool execution did not return a result.");
        }
        return result;
    }

    private ToolExecutionResult executeTool(String toolName, JsonNode argumentsNode) {
        Method method = toolMethods.get(toolName);
        if (method == null) {
            LogUtils.info(buildToolCallLog(toolName, null, "Unknown tool name: " + toolName));
            throw new IllegalArgumentException("Unknown tool name: " + toolName);
        }
        String arguments = "{}";
        if (argumentsNode != null && !argumentsNode.isNull()) {
            try {
                arguments = objectMapper.writeValueAsString(argumentsNode);
            } catch (Exception error) {
                LogUtils.info(buildToolCallLog(toolName, null, "Invalid tool arguments."));
                throw new IllegalArgumentException("Invalid tool arguments.", error);
            }
        }
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name(toolName)
            .arguments(arguments)
            .build();
        DefaultToolExecutor executor = new DefaultToolExecutor(toolSet, method);
        ToolExecutionResult result = executor.executeWithContext(request, InvocationContext.builder().build());
        if (result != null && result.isError()) {
            LogUtils.info(buildToolCallLog(toolName, arguments, result.resultText()));
        }
        return result;
    }

    private String buildToolCallLog(String toolName, String arguments, String errorMessage) {
        String safeToolName = toolName == null ? "unknown tool" : toolName;
        String safeArguments = arguments == null ? "" : arguments;
        String safeError = errorMessage == null ? "" : errorMessage;
        return "MCP tool error: tool=" + safeToolName + ", arguments=" + safeArguments + ", error=" + safeError;
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
