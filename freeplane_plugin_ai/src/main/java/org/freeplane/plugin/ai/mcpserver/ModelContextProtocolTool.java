package org.freeplane.plugin.ai.mcpserver;

import java.util.Map;
import java.util.Objects;

public class ModelContextProtocolTool {
    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;

    public ModelContextProtocolTool(String name, String description, Map<String, Object> inputSchema) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.inputSchema = Objects.requireNonNull(inputSchema, "inputSchema");
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }
}
