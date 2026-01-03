package org.freeplane.plugin.ai.mcpserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fusionauth.http.BodyException;
import io.fusionauth.http.HTTPMethod;
import io.fusionauth.http.HTTPValues.ContentTypes;
import io.fusionauth.http.server.HTTPHandler;
import io.fusionauth.http.server.HTTPListenerConfiguration;
import io.fusionauth.http.server.HTTPRequest;
import io.fusionauth.http.server.HTTPResponse;
import io.fusionauth.http.server.HTTPServer;
import io.fusionauth.http.server.HTTPServerConfiguration;
import dev.langchain4j.service.tool.ToolExecutionResult;
import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.FreeplaneVersion;
import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.ai.tools.AIToolSet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModelContextProtocolServer implements IFreeplanePropertyListener {
    public static final String MCP_SERVER_ENABLED_PROPERTY = "ai_mcp_server_enabled";
    public static final String MCP_SERVER_PORT_PROPERTY = "ai_mcp_server_port";
    private static final String MCP_PROTOCOL_VERSION = "2024-11-05";
    private static final int DEFAULT_PORT = 6298;
    private static final int PORT_MINIMUM = 1024;
    private static final int PORT_MAXIMUM = 65535;
    private static final String SERVER_NAME = "Freeplane AI MCP Server";
    private static final String TOOLS_RESOURCE_URI = "mcp://tools";

    private final ObjectMapper objectMapper;
    private final ModelContextProtocolToolRegistry toolRegistry;
    private final ModelContextProtocolToolDispatcher toolDispatcher;
    private final AtomicBoolean running;
    private volatile HTTPServer server;

    public ModelContextProtocolServer(AIToolSet toolSet) {
        this(toolSet, new ObjectMapper());
    }

    public ModelContextProtocolServer(AIToolSet toolSet, ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.toolRegistry = new ModelContextProtocolToolRegistry(toolSet, this.objectMapper);
        this.toolDispatcher = new ModelContextProtocolToolDispatcher(toolSet, this.objectMapper);
        this.running = new AtomicBoolean(false);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void start() {
        int port = ResourceController.getResourceController()
            .getIntProperty(MCP_SERVER_PORT_PROPERTY, DEFAULT_PORT);
        start(port);
    }

    public void start(int port) {
        if (!isValidPort(port)) {
            LogUtils.severe("MCP server port is invalid: " + port);
            return;
        }
        if (!isPortAvailable(port)) {
            LogUtils.severe("MCP server port is already in use: " + port);
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            HTTPServerConfiguration configuration = new HTTPServerConfiguration()
                .withCompressByDefault(false)
                .withHandler(new ModelContextProtocolHandler())
                .withListener(new HTTPListenerConfiguration(InetAddress.getByName("127.0.0.1"), port));
            @SuppressWarnings("resource")
			HTTPServer httpServer = new HTTPServer().withConfiguration(configuration);
            httpServer.start();
            server = httpServer;
            LogUtils.info("MCP server started on port " + port);
        } catch (Exception error) {
            running.set(false);
            LogUtils.severe("Failed to start MCP server: " + error.getMessage());
        }
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (server != null) {
            server.close();
            server = null;
        }
        LogUtils.info("MCP server stopped.");
    }

    @Override
    public void propertyChanged(String propertyName, String newValue, String oldValue) {
        if (MCP_SERVER_ENABLED_PROPERTY.equals(propertyName)) {
            if (Boolean.parseBoolean(newValue)) {
                start();
            } else {
                stop();
            }
        }
    }

    private boolean isValidPort(int port) {
        return port >= PORT_MINIMUM && port <= PORT_MAXIMUM;
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))) {
            return true;
        } catch (IOException error) {
            return false;
        }
    }

    private class ModelContextProtocolHandler implements HTTPHandler {
        @Override
        public void handle(HTTPRequest request, HTTPResponse response) throws Exception {
            if (request.getMethod() == null || !request.getMethod().is(HTTPMethod.POST)) {
                sendStatus(response, 405);
                return;
            }
            JsonNode requestNode;
            try {
                byte[] bodyBytes = request.getBodyBytes();
                if (bodyBytes == null || bodyBytes.length == 0) {
                    writeJsonResponse(response, buildErrorResponse(null, -32600, "Invalid request"));
                    return;
                }
                requestNode = objectMapper.readTree(bodyBytes);
            } catch (JsonProcessingException error) {
                writeJsonResponse(response, buildErrorResponse(null, -32700, "Parse error"));
                return;
            } catch (BodyException error) {
                writeJsonResponse(response, buildErrorResponse(null, -32700, "Parse error"));
                return;
            }
            if (requestNode == null || requestNode.isNull() || requestNode.isMissingNode()) {
                writeJsonResponse(response, buildErrorResponse(null, -32600, "Invalid request"));
                return;
            }
            if (requestNode.isArray()) {
                writeJsonResponse(response, buildErrorResponse(null, -32600, "Batch requests are not supported"));
                return;
            }
            Object responsePayload = handleRequest(requestNode);
            if (responsePayload == null) {
                sendStatus(response, 204);
                return;
            }
            writeJsonResponse(response, responsePayload);
        }

        private Object handleRequest(JsonNode requestNode) {
            String method = getText(requestNode.get("method"));
            JsonNode idNode = requestNode.get("id");
            Object idValue = idNode == null || idNode.isNull() ? null : objectMapper.convertValue(idNode, Object.class);
            boolean notification = idValue == null;
            if (method == null || method.isEmpty()) {
                return buildErrorResponse(idValue, -32600, "Invalid request");
            }
            if ("initialize".equals(method)) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("protocolVersion", MCP_PROTOCOL_VERSION);
                result.put("capabilities", buildCapabilities());
                result.put("serverInfo", buildServerInfo());
                return buildSuccessResponse(idValue, result, notification);
            }
            if ("notifications/initialized".equals(method)) {
                return null;
            }
            if ("tools/list".equals(method)) {
                return buildSuccessResponse(idValue, buildToolListPayload(), notification);
            }
            if ("resources/list".equals(method)) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("resources", Collections.singletonList(buildToolMetadataResource()));
                return buildSuccessResponse(idValue, result, notification);
            }
            if ("resources/templates/list".equals(method)) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("resourceTemplates", Collections.emptyList());
                return buildSuccessResponse(idValue, result, notification);
            }
            if ("resources/read".equals(method)) {
                return handleResourceRead(requestNode, idValue, notification);
            }
            if ("tools/call".equals(method)) {
                return handleToolCall(requestNode, idValue, notification);
            }
            return buildErrorResponse(idValue, -32601, "Method not found");
        }

        private Object handleToolCall(JsonNode requestNode, Object idValue, boolean notification) {
            JsonNode params = requestNode.get("params");
            String toolName = params == null ? null : getText(params.get("name"));
            if (toolName == null || toolName.isEmpty()) {
                return buildErrorResponse(idValue, -32602, "Missing tool name");
            }
            JsonNode argumentsNode = params == null ? null : params.get("arguments");
            ToolExecutionResult executionResult;
            try {
                executionResult = toolDispatcher.dispatch(toolName, argumentsNode);
            } catch (IllegalArgumentException error) {
                return buildErrorResponse(idValue, -32602, error.getMessage());
            } catch (Exception error) {
                return buildErrorResponse(idValue, -32603, error.getMessage());
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", Collections.singletonList(buildTextContent(executionResult.resultText())));
            if (executionResult.isError()) {
                result.put("isError", true);
            }
            return buildSuccessResponse(idValue, result, notification);
        }

        private Object handleResourceRead(JsonNode requestNode, Object idValue, boolean notification) {
            JsonNode params = requestNode.get("params");
            String resourceIdentifier = params == null ? null : getText(params.get("uri"));
            if (!TOOLS_RESOURCE_URI.equals(resourceIdentifier)) {
                return buildErrorResponse(idValue, -32602, "Unknown resource uri");
            }
            String toolListJson;
            try {
                toolListJson = objectMapper.writeValueAsString(buildToolListPayload());
            } catch (JsonProcessingException error) {
                return buildErrorResponse(idValue, -32603, "Failed to serialize resource");
            }
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("uri", TOOLS_RESOURCE_URI);
            content.put("mimeType", "application/json");
            content.put("text", toolListJson);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("contents", Collections.singletonList(content));
            return buildSuccessResponse(idValue, result, notification);
        }

        private Map<String, Object> buildCapabilities() {
            Map<String, Object> tools = new LinkedHashMap<>();
            tools.put("listChanged", false);
            Map<String, Object> resources = new LinkedHashMap<>();
            resources.put("listChanged", false);
            Map<String, Object> capabilities = new LinkedHashMap<>();
            capabilities.put("tools", tools);
            capabilities.put("resources", resources);
            return capabilities;
        }

        private Map<String, Object> buildToolListPayload() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tools", toolRegistry.listTools());
            return result;
        }

        private Map<String, Object> buildToolMetadataResource() {
            Map<String, Object> resource = new LinkedHashMap<>();
            resource.put("uri", TOOLS_RESOURCE_URI);
            resource.put("name", "Tool metadata");
            resource.put("description", "JSON tool list for MCP clients that only support resources.");
            resource.put("mimeType", "application/json");
            return resource;
        }

        private Map<String, Object> buildServerInfo() {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", SERVER_NAME);
            info.put("version", FreeplaneVersion.getVersion().toString());
            return info;
        }

        private Map<String, Object> buildTextContent(String text) {
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("type", "text");
            content.put("text", text);
            return content;
        }

        private Object buildSuccessResponse(Object idValue, Object result, boolean notification) {
            if (notification) {
                return null;
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", idValue);
            response.put("result", result);
            return response;
        }

        private Object buildErrorResponse(Object idValue, int code, String message) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", code);
            error.put("message", message);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", idValue);
            response.put("error", error);
            return response;
        }

        private String getText(JsonNode node) {
            return node == null || node.isNull() ? null : node.asText();
        }

        private void writeJsonResponse(HTTPResponse response, Object responseBody) throws IOException {
            byte[] responseBytes = objectMapper.writeValueAsBytes(responseBody);
            response.setStatus(200);
            response.setContentType(ContentTypes.ApplicationJson);
            response.setContentLength(responseBytes.length);
            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(responseBytes);
            }
            response.close();
        }

        private void sendStatus(HTTPResponse response, int status) throws IOException {
            response.setStatus(status);
            response.setContentLength(0);
            response.close();
        }
    }
}
