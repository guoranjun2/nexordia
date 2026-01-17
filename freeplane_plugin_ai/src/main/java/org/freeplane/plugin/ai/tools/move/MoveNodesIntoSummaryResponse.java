package org.freeplane.plugin.ai.tools.move;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MoveNodesIntoSummaryResponse {
    private final String mapIdentifier;
    private final String userSummary;
    private final String parentNodeIdentifier;
    private final int insertionIndex;
    private final String summaryNodeIdentifier;

    @JsonCreator
    public MoveNodesIntoSummaryResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                        @JsonProperty("userSummary") String userSummary,
                                        @JsonProperty("parentNodeIdentifier") String parentNodeIdentifier,
                                        @JsonProperty("insertionIndex") int insertionIndex,
                                        @JsonProperty("summaryNodeIdentifier") String summaryNodeIdentifier) {
        this.mapIdentifier = mapIdentifier;
        this.userSummary = userSummary;
        this.parentNodeIdentifier = parentNodeIdentifier;
        this.insertionIndex = insertionIndex;
        this.summaryNodeIdentifier = summaryNodeIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getUserSummary() {
        return userSummary;
    }

    public String getParentNodeIdentifier() {
        return parentNodeIdentifier;
    }

    public int getInsertionIndex() {
        return insertionIndex;
    }

    public String getSummaryNodeIdentifier() {
        return summaryNodeIdentifier;
    }
}
