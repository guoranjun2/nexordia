package org.freeplane.plugin.ai.tools.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeContentItem {
    private final String nodeIdentifier;
    private final NodeContentResponse content;
    @Description("Optional qualifiers when requested: summary_node for summary group nodes, first_group_node for the "
        + "first node of a summary group.")
    private final List<String> qualifiers;

    @JsonCreator
    public NodeContentItem(@JsonProperty("nodeIdentifier") String nodeIdentifier,
                           @JsonProperty("content") NodeContentResponse content,
                           @JsonProperty("qualifiers") List<String> qualifiers) {
        this.nodeIdentifier = nodeIdentifier;
        this.content = content;
        this.qualifiers = qualifiers;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public NodeContentResponse getContent() {
        return content;
    }

    public List<String> getQualifiers() {
        return qualifiers;
    }
}
