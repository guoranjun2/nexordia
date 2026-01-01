package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SetAiOnlyFilterConditionRequest {
    private final String mapIdentifier;
    private final SearchConditionRequest condition;

    @JsonCreator
    public SetAiOnlyFilterConditionRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                           @JsonProperty("condition") SearchConditionRequest condition) {
        this.mapIdentifier = mapIdentifier;
        this.condition = condition;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public SearchConditionRequest getCondition() {
        return condition;
    }
}
