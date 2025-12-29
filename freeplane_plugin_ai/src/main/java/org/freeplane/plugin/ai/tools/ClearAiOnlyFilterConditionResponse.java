package org.freeplane.plugin.ai.tools;

public final class ClearAiOnlyFilterConditionResponse {
    private final String mapIdentifier;
    private final boolean clearsCondition;

    public ClearAiOnlyFilterConditionResponse(String mapIdentifier, boolean clearsCondition) {
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
