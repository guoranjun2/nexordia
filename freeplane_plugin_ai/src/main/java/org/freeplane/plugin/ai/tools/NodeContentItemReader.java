package org.freeplane.plugin.ai.tools;

import java.util.Objects;

import org.freeplane.features.map.NodeModel;

public class NodeContentItemReader {
    private final NodeContentReader nodeContentReader;

    public NodeContentItemReader(NodeContentReader nodeContentReader) {
        this.nodeContentReader = Objects.requireNonNull(nodeContentReader, "nodeContentReader");
    }

    public NodeContentItem readNodeContentItem(NodeModel nodeModel, NodeContentPreset preset) {
        if (nodeModel == null) {
            return null;
        }
        String nodeIdentifier = nodeModel.createID();
        NodeContent content = nodeContentReader.readNodeContent(nodeModel, preset);
        return new NodeContentItem(nodeIdentifier, content);
    }
}
