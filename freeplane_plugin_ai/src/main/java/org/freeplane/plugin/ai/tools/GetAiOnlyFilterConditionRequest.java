package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class GetAiOnlyFilterConditionRequest {
    private final String mapIdentifier;

    @JsonCreator
    public GetAiOnlyFilterConditionRequest(@JsonProperty("mapIdentifier") String mapIdentifier) {
        this.mapIdentifier = mapIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }
}
