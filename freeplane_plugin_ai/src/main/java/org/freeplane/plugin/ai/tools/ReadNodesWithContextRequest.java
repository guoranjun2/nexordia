package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public final class ReadNodesWithContextRequest {
    @Description("Map identifier string.")
    private final String mapIdentifier;
    @JsonProperty(required = false)
    @Description("List of node identifiers. Default: root node.")
    private final List<String> nodeIdentifiers;
    @JsonProperty(required = false)
    @Description("Context sections to include. Default: empty list.")
    private final List<ContextSection> contextSections;
    @JsonProperty(required = false)
    @Description("Depth of full content. Default: 0.")
    private final Integer fullContentDepth;
    @JsonProperty(required = false)
    @Description("Depth of brief summaries beyond fullContentDepth. Default: 1.")
    private final Integer summaryDepth;
    @JsonProperty(required = false)
    @Description("Maximum total response length in characters. Default: 65536.")
    private final Integer maximumTotalTextCharacters;
    @JsonProperty(required = false)
    @Description("NodeContentRequest override for focus node full content.")
    private final NodeContentRequest focusNodeContentRequest;
    @JsonProperty(required = false)
    @Description("NodeContentRequest override for parent node summary.")
    private final NodeContentRequest parentNodeContentRequest;
    @JsonProperty(required = false)
    @Description("NodeContentRequest override for child node full content.")
    private final NodeContentRequest childNodeContentRequest;

    @JsonCreator
    public ReadNodesWithContextRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                       @JsonProperty("nodeIdentifiers") List<String> nodeIdentifiers,
                                       @JsonProperty("contextSections") List<ContextSection> contextSections,
                                       @JsonProperty("fullContentDepth") Integer fullContentDepth,
                                       @JsonProperty("summaryDepth") Integer summaryDepth,
                                       @JsonProperty("maximumTotalTextCharacters") Integer maximumTotalTextCharacters,
                                       @JsonProperty("focusNodeContentRequest") NodeContentRequest focusNodeContentRequest,
                                       @JsonProperty("parentNodeContentRequest") NodeContentRequest parentNodeContentRequest,
                                       @JsonProperty("childNodeContentRequest") NodeContentRequest childNodeContentRequest) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifiers = nodeIdentifiers;
        this.contextSections = contextSections;
        this.fullContentDepth = fullContentDepth;
        this.summaryDepth = summaryDepth;
        this.maximumTotalTextCharacters = maximumTotalTextCharacters;
        this.focusNodeContentRequest = focusNodeContentRequest;
        this.parentNodeContentRequest = parentNodeContentRequest;
        this.childNodeContentRequest = childNodeContentRequest;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getNodeIdentifiers() {
        return nodeIdentifiers;
    }

    public List<ContextSection> getContextSections() {
        return contextSections;
    }

    public Integer getFullContentDepth() {
        return fullContentDepth;
    }

    public Integer getSummaryDepth() {
        return summaryDepth;
    }

    public Integer getMaximumTotalTextCharacters() {
        return maximumTotalTextCharacters;
    }

    public NodeContentRequest getFocusNodeContentRequest() {
        return focusNodeContentRequest;
    }

    public NodeContentRequest getParentNodeContentRequest() {
        return parentNodeContentRequest;
    }

    public NodeContentRequest getChildNodeContentRequest() {
        return childNodeContentRequest;
    }
}
