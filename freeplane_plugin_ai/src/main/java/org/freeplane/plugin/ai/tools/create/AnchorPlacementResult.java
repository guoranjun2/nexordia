package org.freeplane.plugin.ai.tools.create;

import org.freeplane.features.map.NodeModel;

public class AnchorPlacementResult {
    private final NodeModel parentNode;
    private final int insertionIndex;

    public AnchorPlacementResult(NodeModel parentNode, int insertionIndex) {
        this.parentNode = parentNode;
        this.insertionIndex = insertionIndex;
    }

    public NodeModel getParentNode() {
        return parentNode;
    }

    public int getInsertionIndex() {
        return insertionIndex;
    }
}
