package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SearchNodesByConditionRequest {
    private final String mapIdentifier;
    private final SearchConditionRequest condition;
    private final String scopeNodeIdentifier;
    private final int maximumResults;

    @JsonCreator
    public SearchNodesByConditionRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                         @JsonProperty("condition") SearchConditionRequest condition,
                                         @JsonProperty("scopeNodeIdentifier") String scopeNodeIdentifier,
                                         @JsonProperty("maximumResults") int maximumResults) {
        this.mapIdentifier = mapIdentifier;
        this.condition = condition;
        this.scopeNodeIdentifier = scopeNodeIdentifier;
        this.maximumResults = maximumResults;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public SearchConditionRequest getCondition() {
        return condition;
    }

    public String getScopeNodeIdentifier() {
        return scopeNodeIdentifier;
    }

    public int getMaximumResults() {
        return maximumResults;
    }
}
