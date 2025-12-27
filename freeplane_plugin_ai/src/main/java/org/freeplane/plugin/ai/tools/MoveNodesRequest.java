package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class MoveNodesRequest {
    private final String mapIdentifier;
    private final List<String> nodeIdentifiers;
    private final String targetParentIdentifier;
    private final InsertPosition insertPosition;
    private final String referenceNodeIdentifier;
    private final boolean preserveOrder;

    public MoveNodesRequest(String mapIdentifier, List<String> nodeIdentifiers, String targetParentIdentifier,
                            InsertPosition insertPosition, String referenceNodeIdentifier, boolean preserveOrder) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifiers = nodeIdentifiers;
        this.targetParentIdentifier = targetParentIdentifier;
        this.insertPosition = insertPosition;
        this.referenceNodeIdentifier = referenceNodeIdentifier;
        this.preserveOrder = preserveOrder;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getNodeIdentifiers() {
        return nodeIdentifiers;
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

    public boolean isPreserveOrder() {
        return preserveOrder;
    }
}
