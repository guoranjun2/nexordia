package org.freeplane.plugin.ai.tools.move;

import org.freeplane.features.map.AlwaysUnfoldedNode;
import org.freeplane.features.map.FirstGroupNode;
import org.freeplane.features.map.FirstGroupNodeFlag;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.SummaryLevels;
import org.freeplane.features.map.SummaryNode;
import org.freeplane.features.map.SummaryNodeFlag;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.mindmapmode.MModeController;

public class SummaryNodeCreator {
    private final MMapController mapController;

    public SummaryNodeCreator(MMapController mapController) {
        this.mapController = mapController;
    }

    public NodeModel createSummaryNode(NodeModel rootNode, NodeModel firstNode, NodeModel lastNode) {
        if (rootNode == null) {
            throw new IllegalArgumentException("Missing root node.");
        }
        if (firstNode == null || lastNode == null) {
            throw new IllegalArgumentException("Missing summary anchor nodes.");
        }
        NodeModel parentNode = firstNode.getParentNode();
        if (parentNode == null || parentNode != lastNode.getParentNode()) {
            throw new IllegalArgumentException("Summary anchor nodes must share the same parent.");
        }
        int startIndex = parentNode.getIndex(firstNode);
        int endIndex = parentNode.getIndex(lastNode);
        if (startIndex < 0 || endIndex < 0) {
            throw new IllegalArgumentException("Summary anchor nodes are not within the same parent.");
        }
        if (startIndex > endIndex) {
            throw new IllegalArgumentException("Summary anchor nodes are out of order.");
        }
        validateSummaryRange(rootNode, parentNode, startIndex, endIndex);
        NodeModel summaryNode = createSummaryNode(parentNode, endIndex);
        ensureFirstGroupNode(parentNode, startIndex, summaryNode.getSide());
        return summaryNode;
    }

    private void validateSummaryRange(NodeModel rootNode, NodeModel parentNode, int startIndex, int endIndex) {
        SummaryLevels summaryLevels = new SummaryLevels(rootNode, parentNode);
        int summaryLevel = summaryLevels.summaryLevels[startIndex];
        if (summaryLevel != summaryLevels.summaryLevels[endIndex]) {
            throw new IllegalArgumentException("Summary anchors are not on the same summary level.");
        }
        boolean nodesOnOtherSideFound = false;
        boolean isTopOrLeft = parentNode.getChildAt(startIndex).isTopOrLeft(rootNode);
        for (int index = startIndex + 1; index <= endIndex; index++) {
            NodeModel child = parentNode.getChildAt(index);
            boolean nodeIsOnSameSide = isTopOrLeft == child.isTopOrLeft(rootNode);
            if (nodeIsOnSameSide) {
                int level = summaryLevels.summaryLevels[index];
                if (level > summaryLevel || level == summaryLevel && SummaryNode.isFirstGroupNode(child)) {
                    throw new IllegalArgumentException("A child between the anchors already belongs to a summary or is at a higher level.");
                }
            }
            nodesOnOtherSideFound = nodesOnOtherSideFound || !nodeIsOnSameSide;
        }
        if (summaryLevels.findSummaryNodeIndex(endIndex) != SummaryLevels.NODE_NOT_FOUND) {
            throw new IllegalArgumentException("A summary node already covers the requested range.");
        }
        if (nodesOnOtherSideFound) {
            throw new IllegalArgumentException("Summary cannot cross sides; move all nodes to the same side before summarizing.");
        }
    }

    private NodeModel createSummaryNode(NodeModel parentNode, int endIndex) {
        NodeModel endNode = parentNode.getChildAt(endIndex);
        NodeModel summaryNode = new NodeModel("", parentNode.getMap());
        summaryNode.setSide(endNode.getSide());
        mapController.insertNode(summaryNode, parentNode, endIndex + 1);
        MModeController modeController = mapController.getMModeController();
        SummaryNode summaryHook = modeController.getExtension(SummaryNode.class);
        if (summaryHook == null) {
            throw new IllegalStateException("Summary node hook is not available.");
        }
        summaryHook.undoableActivateHook(summaryNode, SummaryNodeFlag.SUMMARY);
        AlwaysUnfoldedNode alwaysUnfoldedNode = modeController.getExtension(AlwaysUnfoldedNode.class);
        if (alwaysUnfoldedNode == null) {
            throw new IllegalStateException("Always unfolded node hook is not available.");
        }
        alwaysUnfoldedNode.undoableActivateHook(summaryNode, alwaysUnfoldedNode);
        return summaryNode;
    }

    private void ensureFirstGroupNode(NodeModel parentNode, int startIndex, NodeModel.Side side) {
        MModeController modeController = mapController.getMModeController();
        FirstGroupNode firstGroupNodeHook = modeController.getExtension(FirstGroupNode.class);
        if (firstGroupNodeHook == null) {
            throw new IllegalStateException("First group node hook is not available.");
        }
        NodeModel startNode = parentNode.getChildAt(startIndex);
        if (SummaryNode.isSummaryNode(startNode)) {
            firstGroupNodeHook.undoableActivateHook(startNode, FirstGroupNodeFlag.FIRST_GROUP);
            return;
        }
        MapModel mapModel = parentNode.getMap();
        NodeModel firstGroupNode = new NodeModel("", mapModel);
        firstGroupNode.setSide(side);
        firstGroupNode.addExtension(FirstGroupNodeFlag.FIRST_GROUP);
        mapController.insertNode(firstGroupNode, parentNode, startIndex);
    }
}
