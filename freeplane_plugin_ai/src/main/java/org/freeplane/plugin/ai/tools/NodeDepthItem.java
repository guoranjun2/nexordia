package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NodeDepthItem {
    private final String nodeIdentifier;
    private final int depth;
    private final NodeContent content;
    @Description("Optional qualifiers when requested: summary_node for summary group nodes, first_group_node for the "
        + "first node of a summary group.")
    private final List<String> qualifiers;

    @JsonCreator
    public NodeDepthItem(@JsonProperty("nodeIdentifier") String nodeIdentifier,
                         @JsonProperty("depth") int depth,
                         @JsonProperty("content") NodeContent content,
                         @JsonProperty("qualifiers") List<String> qualifiers) {
        this.nodeIdentifier = nodeIdentifier;
        this.depth = depth;
        this.content = content;
        this.qualifiers = qualifiers;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public int getDepth() {
        return depth;
    }

    public NodeContent getContent() {
        return content;
    }

    public List<String> getQualifiers() {
        return qualifiers;
    }
}
