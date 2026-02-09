package org.freeplane.plugin.ai.mcpserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.Headers;
import java.util.Map;
import java.util.function.Consumer;
import org.freeplane.core.resources.ResourceController;
import org.junit.Test;

public class ModelContextProtocolAuthValidatorTest {
    private static final String API_KEY_PROPERTY = "ai_mcp_server_api_key";
    private static final String API_KEY_HEADER = "X-Freeplane-MCP-API-Key";

    @Test
    public void blankConfiguredApiKey_generatesAndPersistsAndRejectsCurrentRequest() {
        ResourceController resourceController = mock(ResourceController.class);
        @SuppressWarnings("unchecked")
        Consumer<String> generatedKeyNotifier = mock(Consumer.class);
        when(resourceController.getProperty(API_KEY_PROPERTY, "")).thenReturn("  ");
        ModelContextProtocolAuthValidator uut = new ModelContextProtocolAuthValidator(
            resourceController,
            API_KEY_PROPERTY,
            API_KEY_HEADER,
            () -> "generated-key",
            generatedKeyNotifier);

        Object response = uut.validateRequest(new Headers());

        assertUnauthorized(response);
        verify(resourceController).setProperty(API_KEY_PROPERTY, "generated-key");
        verify(generatedKeyNotifier).accept("generated-key");
    }

    @Test
    public void matchingHeader_allowsRequest() {
        ResourceController resourceController = mock(ResourceController.class);
        @SuppressWarnings("unchecked")
        Consumer<String> generatedKeyNotifier = mock(Consumer.class);
        when(resourceController.getProperty(API_KEY_PROPERTY, "")).thenReturn("expected-key");
        Headers requestHeaders = new Headers();
        requestHeaders.add(API_KEY_HEADER, "expected-key");
        ModelContextProtocolAuthValidator uut = new ModelContextProtocolAuthValidator(
            resourceController,
            API_KEY_PROPERTY,
            API_KEY_HEADER,
            () -> "generated-key",
            generatedKeyNotifier);

        Object response = uut.validateRequest(requestHeaders);

        assertThat(response).isNull();
        verify(resourceController, never()).setProperty(API_KEY_PROPERTY, "generated-key");
        verify(generatedKeyNotifier, never()).accept("generated-key");
    }

    @Test
    public void missingHeader_rejectsRequestWhenApiKeyIsConfigured() {
        ResourceController resourceController = mock(ResourceController.class);
        @SuppressWarnings("unchecked")
        Consumer<String> generatedKeyNotifier = mock(Consumer.class);
        when(resourceController.getProperty(API_KEY_PROPERTY, "")).thenReturn("expected-key");
        ModelContextProtocolAuthValidator uut = new ModelContextProtocolAuthValidator(
            resourceController,
            API_KEY_PROPERTY,
            API_KEY_HEADER,
            () -> "generated-key",
            generatedKeyNotifier);

        Object response = uut.validateRequest(new Headers());

        assertUnauthorized(response);
        verify(resourceController, never()).setProperty(API_KEY_PROPERTY, "generated-key");
        verify(generatedKeyNotifier, never()).accept("generated-key");
    }

    @SuppressWarnings("unchecked")
    private void assertUnauthorized(Object response) {
        assertThat(response).isInstanceOf(Map.class);
        Map<String, Object> responseMap = (Map<String, Object>) response;
        assertThat(responseMap.get("jsonrpc")).isEqualTo("2.0");
        assertThat(responseMap.get("id")).isNull();
        assertThat(responseMap.get("error")).isInstanceOf(Map.class);
        Map<String, Object> error = (Map<String, Object>) responseMap.get("error");
        assertThat(error.get("code")).isEqualTo(-32001);
        assertThat(error.get("message")).isEqualTo("Unauthorized");
    }
}
