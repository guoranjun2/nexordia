package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class NodeCreationItem {
    @Description("Unique index for this node in the nodes list.")
    private final Integer index;
    @Description("Index of the parent node in the same nodes list. Use -1 for a root node. Sibling order follows the "
        + "order of items in the nodes list.")
    private final Integer parentIndex;
    private final NodeContentWriteRequest content;

    @JsonCreator
    public NodeCreationItem(@JsonProperty("index") Integer index,
                            @JsonProperty("parentIndex") Integer parentIndex,
                            @JsonProperty("content") NodeContentWriteRequest content) {
        this.index = index;
        this.parentIndex = parentIndex;
        this.content = content;
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
}
