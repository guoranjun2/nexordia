package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NodeCreationItem {
    private final NodeContent content;
    private final List<NodeCreationItem> children;

    @JsonCreator
    public NodeCreationItem(@JsonProperty("content") NodeContent content,
                            @JsonProperty("children") List<NodeCreationItem> children) {
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
