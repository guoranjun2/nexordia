package org.freeplane.plugin.ai.tools;

public final class SetAiOnlyFilterConditionResponse {
    private final String mapIdentifier;
    private final SearchConditionState activeCondition;

    public SetAiOnlyFilterConditionResponse(String mapIdentifier, SearchConditionState activeCondition) {
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
