package org.freeplane.plugin.ai.mcpserver;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNullSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonReferenceSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ModelContextProtocolToolRegistry {
    private static final Map<String, Object> EMPTY_SCHEMA = createEmptySchema();

    private final Object toolSet;
    private final ObjectMapper objectMapper;

    public ModelContextProtocolToolRegistry(Object toolSet, ObjectMapper objectMapper) {
        this.toolSet = Objects.requireNonNull(toolSet, "toolSet");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public List<ModelContextProtocolTool> listTools() {
        List<ToolSpecification> specifications = ToolSpecifications.toolSpecificationsFrom(toolSet);
        List<ModelContextProtocolTool> tools = new ArrayList<>(specifications.size());
        for (ToolSpecification specification : specifications) {
            Map<String, Object> inputSchema = EMPTY_SCHEMA;
            JsonObjectSchema parameters = specification.parameters();
            if (parameters != null) {
                inputSchema = jsonSchemaElementToMap(parameters);
            }
            tools.add(new ModelContextProtocolTool(
                specification.name(),
                specification.description(),
                inputSchema
            ));
        }
        return tools;
    }

    private Map<String, Object> jsonSchemaElementToMap(JsonSchemaElement schemaElement) {
        if (schemaElement instanceof JsonObjectSchema jsonObjectSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "object");
            if (jsonObjectSchema.description() != null) {
                map.put("description", jsonObjectSchema.description());
            }
            Map<String, Object> properties = new LinkedHashMap<>();
            for (Map.Entry<String, JsonSchemaElement> entry : jsonObjectSchema.properties().entrySet()) {
                properties.put(entry.getKey(), jsonSchemaElementToMap(entry.getValue()));
            }
            map.put("properties", properties);
            if (jsonObjectSchema.required() != null) {
                map.put("required", jsonObjectSchema.required());
            }
            if (jsonObjectSchema.additionalProperties() != null) {
                map.put("additionalProperties", jsonObjectSchema.additionalProperties());
            }
            Map<String, JsonSchemaElement> definitions = jsonObjectSchema.definitions();
            if (definitions != null && !definitions.isEmpty()) {
                Map<String, Object> defs = new LinkedHashMap<>();
                for (Map.Entry<String, JsonSchemaElement> entry : definitions.entrySet()) {
                    defs.put(entry.getKey(), jsonSchemaElementToMap(entry.getValue()));
                }
                map.put("$defs", defs);
            }
            return map;
        }
        if (schemaElement instanceof JsonArraySchema jsonArraySchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "array");
            if (jsonArraySchema.description() != null) {
                map.put("description", jsonArraySchema.description());
            }
            JsonSchemaElement items = jsonArraySchema.items();
            if (items != null) {
                map.put("items", jsonSchemaElementToMap(items));
            }
            return map;
        }
        if (schemaElement instanceof JsonEnumSchema jsonEnumSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "string");
            map.put("enum", jsonEnumSchema.enumValues());
            if (jsonEnumSchema.description() != null) {
                map.put("description", jsonEnumSchema.description());
            }
            return map;
        }
        if (schemaElement instanceof JsonStringSchema jsonStringSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "string");
            if (jsonStringSchema.description() != null) {
                map.put("description", jsonStringSchema.description());
            }
            return map;
        }
        if (schemaElement instanceof JsonIntegerSchema jsonIntegerSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "integer");
            if (jsonIntegerSchema.description() != null) {
                map.put("description", jsonIntegerSchema.description());
            }
            return map;
        }
        if (schemaElement instanceof JsonNumberSchema jsonNumberSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "number");
            if (jsonNumberSchema.description() != null) {
                map.put("description", jsonNumberSchema.description());
            }
            return map;
        }
        if (schemaElement instanceof JsonBooleanSchema jsonBooleanSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "boolean");
            if (jsonBooleanSchema.description() != null) {
                map.put("description", jsonBooleanSchema.description());
            }
            return map;
        }
        if (schemaElement instanceof JsonReferenceSchema jsonReferenceSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("$ref", "#/$defs/" + jsonReferenceSchema.reference());
            return map;
        }
        if (schemaElement instanceof JsonAnyOfSchema jsonAnyOfSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            if (jsonAnyOfSchema.description() != null) {
                map.put("description", jsonAnyOfSchema.description());
            }
            List<Map<String, Object>> anyOf = new ArrayList<>(jsonAnyOfSchema.anyOf().size());
            for (JsonSchemaElement element : jsonAnyOfSchema.anyOf()) {
                anyOf.add(jsonSchemaElementToMap(element));
            }
            map.put("anyOf", anyOf);
            return map;
        }
        if (schemaElement instanceof JsonNullSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "null");
            return map;
        }
        if (schemaElement instanceof JsonRawSchema jsonRawSchema) {
            return parseRawSchema(jsonRawSchema.schema());
        }
        throw new IllegalArgumentException("Unsupported schema element: " + schemaElement.getClass());
    }

    private Map<String, Object> parseRawSchema(String schema) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(schema, Map.class);
            return map;
        } catch (IOException error) {
            throw new IllegalArgumentException("Invalid raw schema JSON.", error);
        }
    }

    private static Map<String, Object> createEmptySchema() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "object");
        map.put("properties", Collections.emptyMap());
        map.put("required", Collections.emptyList());
        return Collections.unmodifiableMap(map);
    }
}
