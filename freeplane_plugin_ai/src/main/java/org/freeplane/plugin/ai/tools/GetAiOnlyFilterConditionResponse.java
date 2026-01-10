package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetAiOnlyFilterConditionResponse {
    private final String mapIdentifier;
    private final SearchConditionState activeCondition;

    @JsonCreator
    public GetAiOnlyFilterConditionResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                            @JsonProperty("activeCondition") SearchConditionState activeCondition) {
        this.mapIdentifier = mapIdentifier;
        this.activeCondition = activeCondition;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public SearchConditionState getActiveCondition() {
        return activeCondition;
    }
}
