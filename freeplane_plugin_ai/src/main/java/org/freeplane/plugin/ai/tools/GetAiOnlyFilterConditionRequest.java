package org.freeplane.plugin.ai.tools;

public final class GetAiOnlyFilterConditionRequest {
    private final String mapIdentifier;

    public GetAiOnlyFilterConditionRequest(String mapIdentifier) {
        this.mapIdentifier = mapIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }
}
