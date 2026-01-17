package org.freeplane.plugin.ai.tools.create;

import org.freeplane.features.map.NodeModel;

public class AnchorPlacementCalculator {
    public AnchorPlacementResult calculatePlacement(NodeModel anchorNode, AnchorPlacementMode placementMode) {
        if (anchorNode == null) {
            throw new IllegalArgumentException("Missing anchor node.");
        }
        if (placementMode == null) {
            throw new IllegalArgumentException("Missing placement mode.");
        }
        switch (placementMode) {
            case FIRST_CHILD:
                return new AnchorPlacementResult(anchorNode, 0);
            case LAST_CHILD:
                return new AnchorPlacementResult(anchorNode, anchorNode.getChildCount());
            case SIBLING_BEFORE:
                return new AnchorPlacementResult(requireParent(anchorNode), anchorNode.getIndex());
            case SIBLING_AFTER:
                return new AnchorPlacementResult(requireParent(anchorNode), anchorNode.getIndex() + 1);
            default:
                throw new IllegalArgumentException("Unsupported placement mode: " + placementMode);
        }
    }

    private NodeModel requireParent(NodeModel anchorNode) {
        NodeModel parentNode = anchorNode.getParentNode();
        if (parentNode == null) {
            throw new IllegalArgumentException("Sibling placement requires a non-root anchor node.");
        }
        return parentNode;
    }
}
