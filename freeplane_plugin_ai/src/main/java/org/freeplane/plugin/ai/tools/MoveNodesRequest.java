package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class MoveNodesRequest {
    private final String mapIdentifier;
    private final List<String> nodeIdentifiers;
    private final String targetParentIdentifier;
    private final InsertPosition insertPosition;
    private final String referenceNodeIdentifier;
    private final boolean preservesOrder;

    @JsonCreator
    public MoveNodesRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                            @JsonProperty("nodeIdentifiers") List<String> nodeIdentifiers,
                            @JsonProperty("targetParentIdentifier") String targetParentIdentifier,
                            @JsonProperty("insertPosition") InsertPosition insertPosition,
                            @JsonProperty("referenceNodeIdentifier") String referenceNodeIdentifier,
                            @JsonProperty("preservesOrder") boolean preservesOrder) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifiers = nodeIdentifiers;
        this.targetParentIdentifier = targetParentIdentifier;
        this.insertPosition = insertPosition;
        this.referenceNodeIdentifier = referenceNodeIdentifier;
        this.preservesOrder = preservesOrder;
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

    public boolean preservesOrder() {
        return preservesOrder;
    }
}
