package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class ApplyAttributesRequest {
    private final String mapIdentifier;
    private final List<String> schema;
    private final List<AttributeUpdate> updates;
    private final String mergeMode;
    private final boolean removesMissing;

    public ApplyAttributesRequest(String mapIdentifier, List<String> schema, List<AttributeUpdate> updates,
                                  String mergeMode, boolean removesMissing) {
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
