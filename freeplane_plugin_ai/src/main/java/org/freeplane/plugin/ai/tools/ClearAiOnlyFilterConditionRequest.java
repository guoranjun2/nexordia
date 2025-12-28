package org.freeplane.plugin.ai.tools;

public final class ClearAiOnlyFilterConditionRequest {
    private final String mapIdentifier;

    public ClearAiOnlyFilterConditionRequest(String mapIdentifier) {
        this.mapIdentifier = mapIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }
}
