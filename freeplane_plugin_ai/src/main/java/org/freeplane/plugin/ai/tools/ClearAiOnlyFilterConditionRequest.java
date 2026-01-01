package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ClearAiOnlyFilterConditionRequest {
    private final String mapIdentifier;

    @JsonCreator
    public ClearAiOnlyFilterConditionRequest(@JsonProperty("mapIdentifier") String mapIdentifier) {
        this.mapIdentifier = mapIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }
}
