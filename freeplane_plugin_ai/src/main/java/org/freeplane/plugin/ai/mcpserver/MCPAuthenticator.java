package org.freeplane.plugin.ai.mcpserver;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.ui.ViewController;

import com.sun.net.httpserver.Headers;

class MCPAuthenticator {
    private static final int UNAUTHORIZED_ERROR_CODE = -32001;
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    private static final String GENERATED_MCP_TOKEN_MESSAGE = "ai_mcp_token_generated_message";

    private final ResourceController resourceController;
    private final String apiKeyPropertyName;
    private final String apiKeyHeaderName;
    private final EventDispatchInvoker eventDispatchInvoker;

    MCPAuthenticator(ResourceController resourceController, ViewController viewController,
                     String apiKeyPropertyName, String apiKeyHeaderName) {
        this(resourceController, apiKeyPropertyName, apiKeyHeaderName, viewController::invokeAndWait);
    }

    MCPAuthenticator(ResourceController resourceController, String apiKeyPropertyName,
                     String apiKeyHeaderName, EventDispatchInvoker eventDispatchInvoker) {
        this.resourceController = resourceController;
        this.apiKeyPropertyName = apiKeyPropertyName;
        this.apiKeyHeaderName = apiKeyHeaderName;
        this.eventDispatchInvoker = eventDispatchInvoker;
    }

    Object authenticateRequest(Headers requestHeaders) {
        AtomicReference<Object> responseReference = new AtomicReference<>();
        AtomicReference<Throwable> errorReference = new AtomicReference<>();
        Runnable runnable = () -> {
            try {
                responseReference.set(authenticateOnEdt(requestHeaders));
            } catch (Throwable throwable) {
                errorReference.set(throwable);
            }
        };
        try {
            eventDispatchInvoker.invokeAndWait(runnable);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("MCP authentication was interrupted.", interruptedException);
        } catch (InvocationTargetException invocationTargetException) {
            throw toRuntimeException(invocationTargetException.getCause(), "MCP authentication failed.");
        }
        Throwable error = errorReference.get();
        if (error != null) {
            throw toRuntimeException(error, "MCP authentication failed.");
        }
        return responseReference.get();
    }

    private Object authenticateOnEdt(Headers requestHeaders) {
        String configuredApiKey = trimToEmpty(resourceController.getProperty(apiKeyPropertyName, ""));
        if (configuredApiKey.isEmpty()) {
            String generatedApiKey = generateToken();
            resourceController.setProperty(apiKeyPropertyName, generatedApiKey);
            notifyTokenGenerated(generatedApiKey);
            return buildUnauthorizedResponse();
        }
        String providedApiKey = trimToEmpty(requestHeaders == null ? null : requestHeaders.getFirst(apiKeyHeaderName));
        if (!configuredApiKey.equals(providedApiKey)) {
            return buildUnauthorizedResponse();
        }
        return null;
    }

    String generateToken() {
        return UUID.randomUUID().toString();
    }

    void notifyTokenGenerated(String generatedApiKey) {
        UITools.showMessage(
            TextUtils.format(GENERATED_MCP_TOKEN_MESSAGE, generatedApiKey),
            JOptionPane.INFORMATION_MESSAGE);
    }

    private RuntimeException toRuntimeException(Throwable throwable, String fallbackMessage) {
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(fallbackMessage, throwable);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private Object buildUnauthorizedResponse() {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", UNAUTHORIZED_ERROR_CODE);
        error.put("message", UNAUTHORIZED_MESSAGE);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", null);
        response.put("error", error);
        return response;
    }

    @FunctionalInterface
    interface EventDispatchInvoker {
        void invokeAndWait(Runnable runnable) throws InterruptedException, InvocationTargetException;
    }
}
