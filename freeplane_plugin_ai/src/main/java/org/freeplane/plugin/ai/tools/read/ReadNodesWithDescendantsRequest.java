package org.freeplane.plugin.ai.tools.read;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class ReadNodesWithDescendantsRequest {
    private static final int DEFAULT_FULL_CONTENT_DEPTH = 0;
    private static final int DEFAULT_SUMMARY_DEPTH = 1;
    private static final int DEFAULT_MAXIMUM_TOTAL_TEXT_CHARACTERS = 65536;
    @Description("Map identifier string. Use another tool call to refresh identifiers if needed.")
    private final String mapIdentifier;
    @JsonProperty(required = false)
    @Description("List of node identifiers. Default: root node. Use another tool call to refresh identifiers "
        + "if needed.")
    private final List<String> nodeIdentifiers;
    @JsonProperty(required = false)
    @Description("Context sections to include. Default: empty list. QUALIFIERS adds qualifier strings such as "
        + "summary_node and first_group_node. HYPERLINK, OUTGOING_CONNECTORS, INCOMING_CONNECTORS, and CLONE_METADATA "
        + "opt in to additional link and clone metadata.")
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
    private final boolean hasFullContentDepth;
    private final boolean hasSummaryDepth;
    private final boolean hasMaximumTotalTextCharacters;

    @JsonCreator
    public ReadNodesWithDescendantsRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                       @JsonProperty("nodeIdentifiers") List<String> nodeIdentifiers,
                                       @JsonProperty("contextSections") List<ContextSection> contextSections,
                                       @JsonProperty("fullContentDepth") Integer fullContentDepth,
                                       @JsonProperty("summaryDepth") Integer summaryDepth,
                                       @JsonProperty("maximumTotalTextCharacters") Integer maximumTotalTextCharacters) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifiers = nodeIdentifiers;
        this.contextSections = normalizeContextSections(contextSections);
        this.hasFullContentDepth = fullContentDepth != null;
        this.fullContentDepth = fullContentDepth == null ? DEFAULT_FULL_CONTENT_DEPTH : fullContentDepth;
        this.hasSummaryDepth = summaryDepth != null;
        this.summaryDepth = summaryDepth == null ? DEFAULT_SUMMARY_DEPTH : summaryDepth;
        this.hasMaximumTotalTextCharacters = maximumTotalTextCharacters != null;
        this.maximumTotalTextCharacters = maximumTotalTextCharacters == null
            ? DEFAULT_MAXIMUM_TOTAL_TEXT_CHARACTERS
            : maximumTotalTextCharacters;
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

    public boolean hasFullContentDepth() {
        return hasFullContentDepth;
    }

    public boolean hasSummaryDepth() {
        return hasSummaryDepth;
    }

    public boolean hasMaximumTotalTextCharacters() {
        return hasMaximumTotalTextCharacters;
    }

    private static List<ContextSection> normalizeContextSections(List<ContextSection> contextSections) {
        if (contextSections == null || contextSections.isEmpty()) {
            return Collections.emptyList();
        }
        List<ContextSection> normalized = new ArrayList<>();
        for (ContextSection section : contextSections) {
            if (section != null) {
                normalized.add(section);
            }
        }
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(normalized);
    }
}
