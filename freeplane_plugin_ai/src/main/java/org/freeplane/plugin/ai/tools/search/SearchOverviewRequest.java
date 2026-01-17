package org.freeplane.plugin.ai.tools.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchOverviewRequest {
    private final String mapIdentifier;
    private final String focusRequest;
    private final String modelIdentifier;
    private final int maximumKeywordCount;
    private final int maximumSectionCount;

    @JsonCreator
    public SearchOverviewRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                 @JsonProperty("focusRequest") String focusRequest,
                                 @JsonProperty("modelIdentifier") String modelIdentifier,
                                 @JsonProperty("maximumKeywordCount") int maximumKeywordCount,
                                 @JsonProperty("maximumSectionCount") int maximumSectionCount) {
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
