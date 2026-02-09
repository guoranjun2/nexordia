package org.freeplane.plugin.ai.mcpserver;

import com.sun.net.httpserver.Headers;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.TextUtils;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

class ModelContextProtocolAuthValidator {
    private static final int UNAUTHORIZED_ERROR_CODE = -32001;
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    private static final String GENERATED_MCP_TOKEN_MESSAGE = "ai_mcp_token_generated_message";

    private final ResourceController resourceController;
    private final String apiKeyPropertyName;
    private final String apiKeyHeaderName;
    private final Supplier<String> apiKeyGenerator;
    private final Consumer<String> apiKeyNotifier;

    ModelContextProtocolAuthValidator(ResourceController resourceController, String apiKeyPropertyName,
                                      String apiKeyHeaderName) {
        this(resourceController, apiKeyPropertyName, apiKeyHeaderName,
            () -> UUID.randomUUID().toString(),
            apiKey -> SwingUtilities.invokeLater(() ->
                UITools.showMessage(
                    TextUtils.format(GENERATED_MCP_TOKEN_MESSAGE, apiKey),
                    JOptionPane.INFORMATION_MESSAGE)));
    }

    ModelContextProtocolAuthValidator(ResourceController resourceController, String apiKeyPropertyName,
                                      String apiKeyHeaderName, Supplier<String> apiKeyGenerator,
                                      Consumer<String> apiKeyNotifier) {
        this.resourceController = resourceController;
        this.apiKeyPropertyName = apiKeyPropertyName;
        this.apiKeyHeaderName = apiKeyHeaderName;
        this.apiKeyGenerator = apiKeyGenerator;
        this.apiKeyNotifier = apiKeyNotifier;
    }

    Object validateRequest(Headers requestHeaders) {
        String configuredApiKey = trimToEmpty(resourceController.getProperty(apiKeyPropertyName, ""));
        if (configuredApiKey.isEmpty()) {
            String generatedApiKey = generateApiKey();
            resourceController.setProperty(apiKeyPropertyName, generatedApiKey);
            apiKeyNotifier.accept(generatedApiKey);
            return buildUnauthorizedResponse();
        }
        String providedApiKey = trimToEmpty(requestHeaders == null ? null : requestHeaders.getFirst(apiKeyHeaderName));
        if (!configuredApiKey.equals(providedApiKey)) {
            return buildUnauthorizedResponse();
        }
        return null;
    }

    private String generateApiKey() {
        String generatedApiKey = trimToEmpty(apiKeyGenerator.get());
        if (!generatedApiKey.isEmpty()) {
            return generatedApiKey;
        }
        return UUID.randomUUID().toString();
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
}
