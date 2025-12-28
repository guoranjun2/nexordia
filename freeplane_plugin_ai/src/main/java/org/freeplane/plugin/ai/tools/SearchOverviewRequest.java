package org.freeplane.plugin.ai.tools;

public final class SearchOverviewRequest {
    private final String mapIdentifier;
    private final String focusRequest;
    private final String modelIdentifier;
    private final int maximumKeywordCount;
    private final int maximumSectionCount;

    public SearchOverviewRequest(String mapIdentifier, String focusRequest, String modelIdentifier,
                                 int maximumKeywordCount, int maximumSectionCount) {
        this.mapIdentifier = mapIdentifier;
        this.focusRequest = focusRequest;
        this.modelIdentifier = modelIdentifier;
        this.maximumKeywordCount = maximumKeywordCount;
        this.maximumSectionCount = maximumSectionCount;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getFocusRequest() {
        return focusRequest;
    }

    public String getModelIdentifier() {
        return modelIdentifier;
    }

    public int getMaximumKeywordCount() {
        return maximumKeywordCount;
    }

    public int getMaximumSectionCount() {
        return maximumSectionCount;
    }
}
