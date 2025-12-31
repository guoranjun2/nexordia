package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class ReadNodeContentResponse {
    private final String mapIdentifier;
    private final NodeContentItem focusNode;
    private final NodeContentItem parentNode;
    private final List<NodeContentItem> childNodes;

    public ReadNodeContentResponse(String mapIdentifier, NodeContentItem focusNode, NodeContentItem parentNode,
                                   List<NodeContentItem> childNodes) {
        this.mapIdentifier = mapIdentifier;
        this.focusNode = focusNode;
        this.parentNode = parentNode;
        this.childNodes = childNodes;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public NodeContentItem getFocusNode() {
        return focusNode;
    }

    public NodeContentItem getParentNode() {
        return parentNode;
    }

    public List<NodeContentItem> getChildNodes() {
        return childNodes;
    }
}
