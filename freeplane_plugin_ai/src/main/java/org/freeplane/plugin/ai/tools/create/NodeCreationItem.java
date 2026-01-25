package org.freeplane.plugin.ai.tools.create;

import org.freeplane.plugin.ai.tools.content.NodeContentWriteRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.langchain4j.model.output.structured.Description;

public class NodeCreationItem {
    @Description("Item index in the nodes list.")
    private final Integer index;
    @Description("Parent index in this list (-1 uses anchor).")
    private final Integer parentIndex;
    private final NodeContentWriteRequest content;
    @JsonProperty(required = false)
    @Description("Optional folding state for new non-leaf nodes (default: UNFOLD).")
    private final NodeFoldingState foldingState;

    @JsonCreator
    public NodeCreationItem(@JsonProperty("index") Integer index,
                            @JsonProperty("parentIndex") Integer parentIndex,
                            @JsonProperty("content") NodeContentWriteRequest content,
                            @JsonProperty(value = "foldingState", required = false) NodeFoldingState foldingState) {
        this.index = index;
        this.parentIndex = parentIndex;
        this.content = content;
        this.foldingState = foldingState;
    }

    public Integer getIndex() {
        return index;
    }

    public Integer getParentIndex() {
        return parentIndex;
    }

    public NodeContentWriteRequest getContent() {
        return content;
    }

    public NodeFoldingState getFoldingState() {
        return foldingState;
    }
}
