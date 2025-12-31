package org.freeplane.plugin.ai.tools;

public final class NodeContentItem {
    private final String nodeIdentifier;
    private final NodeContent content;

    public NodeContentItem(String nodeIdentifier, NodeContent content) {
        this.nodeIdentifier = nodeIdentifier;
        this.content = content;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public NodeContent getContent() {
        return content;
    }
}
