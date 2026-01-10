package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClearAiOnlyFilterConditionResponse {
    private final String mapIdentifier;
    private final boolean clearsCondition;

    @JsonCreator
    public ClearAiOnlyFilterConditionResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                              @JsonProperty("clearsCondition") boolean clearsCondition) {
        this.mapIdentifier = mapIdentifier;
        this.clearsCondition = clearsCondition;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public boolean clearsCondition() {
        return clearsCondition;
    }
}
