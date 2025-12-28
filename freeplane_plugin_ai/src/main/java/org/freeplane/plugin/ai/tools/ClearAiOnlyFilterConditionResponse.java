package org.freeplane.plugin.ai.tools;

public final class ClearAiOnlyFilterConditionResponse {
    private final String mapIdentifier;
    private final boolean cleared;

    public ClearAiOnlyFilterConditionResponse(String mapIdentifier, boolean cleared) {
        this.mapIdentifier = mapIdentifier;
        this.cleared = cleared;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public boolean isCleared() {
        return cleared;
    }
}
