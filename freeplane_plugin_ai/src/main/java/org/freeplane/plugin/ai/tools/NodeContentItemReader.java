package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.SummaryNode;

public class NodeContentItemReader {
    private final NodeContentReader nodeContentReader;

    public NodeContentItemReader(NodeContentReader nodeContentReader) {
        this.nodeContentReader = Objects.requireNonNull(nodeContentReader, "nodeContentReader");
    }

    public NodeContentItem readNodeContentItem(NodeModel nodeModel, NodeContentPreset preset) {
        return readNodeContentItem(nodeModel, preset, true, true);
    }

    public NodeContentItem readNodeContentItem(NodeModel nodeModel, NodeContentPreset preset,
                                               boolean includesNodeIdentifiers) {
        return readNodeContentItem(nodeModel, preset, includesNodeIdentifiers, true);
    }

    public NodeContentItem readNodeContentItem(NodeModel nodeModel, NodeContentPreset preset,
                                               boolean includesNodeIdentifiers, boolean includesQualifiers) {
        if (nodeModel == null) {
            return null;
        }
        String nodeIdentifier = includesNodeIdentifiers ? nodeModel.createID() : null;
        NodeContent content = nodeContentReader.readNodeContent(nodeModel, preset);
        List<String> qualifiers = includesQualifiers ? buildQualifiers(nodeModel) : null;
        return new NodeContentItem(nodeIdentifier, content, qualifiers);
    }

    public NodeContentItem readNodeContentItem(NodeModel nodeModel, NodeContent content,
                                               boolean includesNodeIdentifiers) {
        return readNodeContentItem(nodeModel, content, includesNodeIdentifiers, true);
    }

    public NodeContentItem readNodeContentItem(NodeModel nodeModel, NodeContent content,
                                               boolean includesNodeIdentifiers, boolean includesQualifiers) {
        if (nodeModel == null) {
            return null;
        }
        String nodeIdentifier = includesNodeIdentifiers ? nodeModel.createID() : null;
        List<String> qualifiers = includesQualifiers ? buildQualifiers(nodeModel) : null;
        return new NodeContentItem(nodeIdentifier, content, qualifiers);
    }

    public NodeContent readNodeContent(NodeModel nodeModel, NodeContentRequest request, NodeContentPreset fallbackPreset) {
        return nodeContentReader.readNodeContent(nodeModel, request, fallbackPreset);
    }

    private List<String> buildQualifiers(NodeModel nodeModel) {
        List<String> qualifiers = new ArrayList<>();
        if (SummaryNode.isSummaryNode(nodeModel)) {
            qualifiers.add("summary_node");
        }
        if (SummaryNode.isFirstGroupNode(nodeModel)) {
            qualifiers.add("first_group_node");
        }
        if (qualifiers.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(qualifiers);
    }
}
