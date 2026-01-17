package org.freeplane.plugin.ai.tools.edit;

import java.util.List;

import org.freeplane.plugin.ai.tools.content.AttributeUpdate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApplyAttributesRequest {
    private final String mapIdentifier;
    private final List<String> schema;
    private final List<AttributeUpdate> updates;
    private final String mergeMode;
    private final boolean removesMissing;

    @JsonCreator
    public ApplyAttributesRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                  @JsonProperty("schema") List<String> schema,
                                  @JsonProperty("updates") List<AttributeUpdate> updates,
                                  @JsonProperty("mergeMode") String mergeMode,
                                  @JsonProperty("removesMissing") boolean removesMissing) {
        this.mapIdentifier = mapIdentifier;
        this.schema = schema;
        this.updates = updates;
        this.mergeMode = mergeMode;
        this.removesMissing = removesMissing;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getSchema() {
        return schema;
    }

    public List<AttributeUpdate> getUpdates() {
        return updates;
    }

    public String getMergeMode() {
        return mergeMode;
    }

    public boolean removesMissing() {
        return removesMissing;
    }
}
