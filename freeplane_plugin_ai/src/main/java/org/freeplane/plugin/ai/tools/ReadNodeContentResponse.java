package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ReadNodeContentResponse {
    private final String mapIdentifier;
    private final NodeContentItem focusNode;
    private final NodeContentItem parentNode;
    private final List<NodeContentItem> childNodes;

    @JsonCreator
    public ReadNodeContentResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                   @JsonProperty("focusNode") NodeContentItem focusNode,
                                   @JsonProperty("parentNode") NodeContentItem parentNode,
                                   @JsonProperty("childNodes") List<NodeContentItem> childNodes) {
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
