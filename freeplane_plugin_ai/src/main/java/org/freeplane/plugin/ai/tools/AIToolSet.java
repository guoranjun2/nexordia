package org.freeplane.plugin.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import org.freeplane.plugin.ai.chat.SystemMessageBuilder;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.freeplane.plugin.ai.maps.ControllerMapModelProvider;

public class AIToolSet {
    private final SystemMessageBuilder systemMessageBuilder;

    public AIToolSet() {
        this(new SystemMessageBuilder(new AvailableMaps(new ControllerMapModelProvider())));
    }

    AIToolSet(SystemMessageBuilder systemMessageBuilder) {
        this.systemMessageBuilder = systemMessageBuilder;
    }

    public String systemMessageForChat(Object input) {
        return systemMessageBuilder.buildForChat();
    }

    @Tool("Get breadcrumbs from the root to a node.")
    public BreadcrumbsResponse getBreadcrumbs(BreadcrumbsRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Return a flat list of nodes under a branch.")
    public FlatListResponse getFlatList(FlatListRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("List properties that can be used for search and filter conditions, excluding attributes.")
    public SearchPropertiesResponse listSearchProperties(SearchPropertiesRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("List valid conditions and value input modes for a property.")
    public SearchConditionsForPropertyResponse listSearchConditionsForProperty(
            SearchConditionsForPropertyRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Search nodes using a condition based on search and filter properties.")
    public SearchNodesByConditionResponse searchNodesByCondition(SearchNodesByConditionRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("List attribute names available for a map.")
    public AttributeNamesForMapResponse listAttributeNamesForMap(AttributeNamesForMapRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Search nodes by attribute name and value.")
    public SearchAttributesByNameAndValueResponse searchAttributesByNameAndValue(
            SearchAttributesByNameAndValueRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Generate a compact overview and index for targeted search.")
    public SearchOverviewResponse generateSearchOverview(SearchOverviewRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Set an AI only filter condition that affects only AI tool calls.")
    public SetAiOnlyFilterConditionResponse setAiOnlyFilterCondition(SetAiOnlyFilterConditionRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Get the active AI only filter condition.")
    public GetAiOnlyFilterConditionResponse getAiOnlyFilterCondition(GetAiOnlyFilterConditionRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Clear the active AI only filter condition.")
    public ClearAiOnlyFilterConditionResponse clearAiOnlyFilterCondition(ClearAiOnlyFilterConditionRequest request) {
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
