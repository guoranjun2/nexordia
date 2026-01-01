package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class AttributeNamesForMapRequest {
    private final String mapIdentifier;

    @JsonCreator
    public AttributeNamesForMapRequest(@JsonProperty("mapIdentifier") String mapIdentifier) {
        this.mapIdentifier = mapIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }
}
