package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CreateNodesRequest {
    private final String mapIdentifier;
    private final String targetParentIdentifier;
    private final InsertPosition insertPosition;
    private final String referenceNodeIdentifier;
    private final List<NodeCreationItem> nodes;

    @JsonCreator
    public CreateNodesRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                              @JsonProperty("targetParentIdentifier") String targetParentIdentifier,
                              @JsonProperty("insertPosition") InsertPosition insertPosition,
                              @JsonProperty("referenceNodeIdentifier") String referenceNodeIdentifier,
                              @JsonProperty("nodes") List<NodeCreationItem> nodes) {
        this.mapIdentifier = mapIdentifier;
        this.targetParentIdentifier = targetParentIdentifier;
        this.insertPosition = insertPosition;
        this.referenceNodeIdentifier = referenceNodeIdentifier;
        this.nodes = nodes;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getTargetParentIdentifier() {
        return targetParentIdentifier;
    }

    public InsertPosition getInsertPosition() {
        return insertPosition;
    }

    public String getReferenceNodeIdentifier() {
        return referenceNodeIdentifier;
    }

    public List<NodeCreationItem> getNodes() {
        return nodes;
    }
}
