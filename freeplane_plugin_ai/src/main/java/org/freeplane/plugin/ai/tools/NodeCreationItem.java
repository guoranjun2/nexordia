package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class NodeCreationItem {
    private final NodeContent content;
    private final List<NodeCreationItem> children;

    public NodeCreationItem(NodeContent content, List<NodeCreationItem> children) {
        this.content = content;
        this.children = children;
    }

    public NodeContent getContent() {
        return content;
    }

    public List<NodeCreationItem> getChildren() {
        return children;
    }
}
