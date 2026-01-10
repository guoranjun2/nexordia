package org.freeplane.plugin.ai.tools;

import java.util.List;
import java.util.stream.Collectors;

import org.freeplane.features.map.NodeModel;

public class MoveErrorDescription {

    private MoveErrorDescription() {
    }

    public static String describe(List<NodeModel> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return "";
        }
        return " [nodes=" + nodes.stream().map(NodeModel::createID).collect(Collectors.joining(",")) + "]";
    }
}
