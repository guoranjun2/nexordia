package org.freeplane.plugin.ai.tools;

public final class SetAiOnlyFilterConditionRequest {
    private final String mapIdentifier;
    private final SearchConditionRequest condition;

    public SetAiOnlyFilterConditionRequest(String mapIdentifier, SearchConditionRequest condition) {
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
