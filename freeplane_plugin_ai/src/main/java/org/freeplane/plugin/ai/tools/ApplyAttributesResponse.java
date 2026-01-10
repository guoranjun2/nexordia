package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApplyAttributesResponse {
    private final String mapIdentifier;
    private final List<String> updatedNodeIdentifiers;

    @JsonCreator
    public ApplyAttributesResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                   @JsonProperty("updatedNodeIdentifiers") List<String> updatedNodeIdentifiers) {
        this.mapIdentifier = mapIdentifier;
        this.updatedNodeIdentifiers = updatedNodeIdentifiers;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getUpdatedNodeIdentifiers() {
        return updatedNodeIdentifiers;
    }
}
