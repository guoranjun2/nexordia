package org.freeplane.plugin.ai.tools;

public final class GetAiOnlyFilterConditionResponse {
    private final String mapIdentifier;
    private final SearchConditionState activeCondition;

    public GetAiOnlyFilterConditionResponse(String mapIdentifier, SearchConditionState activeCondition) {
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
