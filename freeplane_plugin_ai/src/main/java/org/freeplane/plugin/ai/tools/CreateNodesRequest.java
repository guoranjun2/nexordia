package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class CreateNodesRequest {
    private final String mapIdentifier;
    private final String targetParentIdentifier;
    private final InsertPosition insertPosition;
    private final String referenceNodeIdentifier;
    private final List<NodeCreationItem> nodes;

    public CreateNodesRequest(String mapIdentifier, String targetParentIdentifier, InsertPosition insertPosition,
                              String referenceNodeIdentifier, List<NodeCreationItem> nodes) {
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
