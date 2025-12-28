package org.freeplane.plugin.ai.tools;

public final class SearchNodesByConditionRequest {
    private final String mapIdentifier;
    private final SearchConditionRequest condition;
    private final String scopeNodeIdentifier;
    private final int maximumResults;

    public SearchNodesByConditionRequest(String mapIdentifier, SearchConditionRequest condition,
                                         String scopeNodeIdentifier, int maximumResults) {
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
