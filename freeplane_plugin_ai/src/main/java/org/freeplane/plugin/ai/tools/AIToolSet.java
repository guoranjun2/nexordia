package org.freeplane.plugin.ai.tools;

import dev.langchain4j.agent.tool.Tool;

public class AIToolSet {

    public String systemMessageForChat(Object input) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Read node context with optional layers and depth.")
    public NodeContextResponse readNodeContext(NodeContextRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Get breadcrumbs from the root to a node.")
    public BreadcrumbsResponse getBreadcrumbs(BreadcrumbsRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Search nodes by text.")
    public SearchMapResponse searchMap(SearchMapRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Return a flat list of nodes under a branch.")
    public FlatListResponse getFlatList(FlatListRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Create nodes and subtrees under a target parent.")
    public CreateNodesResponse createNodes(CreateNodesRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Apply attributes to selected nodes.")
    public ApplyAttributesResponse applyAttributes(ApplyAttributesRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Move nodes under a new parent.")
    public MoveNodesResponse moveNodes(MoveNodesRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
