package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ReadNodesWithContextRequest {
    private final String mapIdentifier;
    private final List<String> nodeIdentifiers;
    private final List<ContextSection> contextSections;
    private final Integer fullContentDepth;
    private final Integer summaryDepth;
    private final Integer maximumTotalTextCharacters;
    private final NodeContentRequest focusNodeContentRequest;
    private final NodeContentRequest parentNodeContentRequest;
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
