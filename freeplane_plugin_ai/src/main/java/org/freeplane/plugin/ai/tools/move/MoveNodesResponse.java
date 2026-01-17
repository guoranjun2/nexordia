package org.freeplane.plugin.ai.tools.move;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MoveNodesResponse {
    private final String mapIdentifier;
    private final String userSummary;
    private final String parentNodeIdentifier;
    private final int insertionIndex;

    @JsonCreator
    public MoveNodesResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                             @JsonProperty("userSummary") String userSummary,
                             @JsonProperty("parentNodeIdentifier") String parentNodeIdentifier,
                             @JsonProperty("insertionIndex") int insertionIndex) {
        this.mapIdentifier = mapIdentifier;
        this.userSummary = userSummary;
        this.parentNodeIdentifier = parentNodeIdentifier;
        this.insertionIndex = insertionIndex;
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
}
