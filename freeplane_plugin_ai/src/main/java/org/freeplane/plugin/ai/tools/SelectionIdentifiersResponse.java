package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class SelectionIdentifiersResponse {
    @Description("Selected map identifier string.")
    private final String mapIdentifier;
    @Description("Identifier of the primary selected node.")
    private final String nodeIdentifier;
    @Description("Root node identifier for the selected map.")
    private final String rootNodeIdentifier;
    @Description("Selected nodes with short text previews.")
    private final List<SelectedNodeSummary> selectedNodes;
    @Description("Total number of selected nodes.")
    private final int selectedNodeCount;
    @Description("Total number of unique selected subtrees.")
    private final int selectedUniqueSubtreeCount;

    @JsonCreator
    public SelectionIdentifiersResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                        @JsonProperty("nodeIdentifier") String nodeIdentifier,
                                        @JsonProperty("rootNodeIdentifier") String rootNodeIdentifier,
                                        @JsonProperty("selectedNodes") List<SelectedNodeSummary> selectedNodes,
                                        @JsonProperty("selectedNodeCount") int selectedNodeCount,
                                        @JsonProperty("selectedUniqueSubtreeCount") int selectedUniqueSubtreeCount) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifier = nodeIdentifier;
        this.rootNodeIdentifier = rootNodeIdentifier;
        this.selectedNodes = selectedNodes;
        this.selectedNodeCount = selectedNodeCount;
        this.selectedUniqueSubtreeCount = selectedUniqueSubtreeCount;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public String getRootNodeIdentifier() {
        return rootNodeIdentifier;
    }

    public List<SelectedNodeSummary> getSelectedNodes() {
        return selectedNodes;
    }

    public int getSelectedNodeCount() {
        return selectedNodeCount;
    }

    public int getSelectedUniqueSubtreeCount() {
        return selectedUniqueSubtreeCount;
    }
}
