package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.langchain4j.agent.tool.Tool;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.chat.SystemMessageBuilder;
import org.freeplane.plugin.ai.maps.AvailableMaps;

public class AIToolSet {
    private final SystemMessageBuilder systemMessageBuilder;
    private final ReadNodeWithContextTool readNodeWithContextTool;
    private final SelectedMapAndNodeIdentifiersTool selectedMapAndNodeIdentifiersTool;
    private final SearchNodesTool searchNodesTool;
    private final CreateNodesTool createNodesTool;
    private final MoveNodesTool moveNodesTool;
    private final CreateSummaryTool createSummaryTool;
    private final MoveNodesIntoSummaryTool moveNodesIntoSummaryTool;
    private final ToolCallSummaryHandler toolCallSummaryHandler;

    AIToolSet(ToolCallSummaryHandler toolCallSummaryHandler, AvailableMaps availableMaps, TextController textController,
              NodeContentFactories nodeContentFactories, MMapController mapController) {
        Objects.requireNonNull(mapController, "mapController");
        NodeModelCreator nodeModelCreator = new NodeModelCreator();
        AnchorPlacementCalculator anchorPlacementCalculator = new AnchorPlacementCalculator();
        NodeInserter nodeInserter = new NodeInserter(mapController, anchorPlacementCalculator);
        SummaryNodeCreator summaryNodeCreator = new SummaryNodeCreator(mapController);
        ModifiedNodeSummaryBuilder modifiedNodeSummaryBuilder = new ModifiedNodeSummaryBuilder(textController);
        TextualContentEditor textualContentEditor = new TextualContentEditor();
        AttributesContentEditor attributesContentEditor = new AttributesContentEditor();
        TagsContentEditor tagsContentEditor = new TagsContentEditor();
        List<NamedIcon> iconCandidates = new ArrayList<>(IconStoreFactory.ICON_STORE.getMindIcons());
        iconCandidates.addAll(IconStoreFactory.ICON_STORE.getUserIcons());
        IconsContentEditor iconsContentEditor = new IconsContentEditor(
            nodeContentFactories.iconDescriptionResolver, iconCandidates);
        NodeContentApplier nodeContentApplier = new NodeContentApplier(textualContentEditor, attributesContentEditor,
            tagsContentEditor, iconsContentEditor);
        SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
        ReadNodeWithContextTool readNodeWithContextTool = new ReadNodeWithContextTool(
            availableMaps, nodeContentFactories.nodeContentItemReader, textController);
        SelectedMapAndNodeIdentifiersTool selectedMapAndNodeIdentifiersTool = new SelectedMapAndNodeIdentifiersTool(
            availableMaps);
        SearchNodesTool searchNodesTool = new SearchNodesTool(availableMaps, nodeContentFactories.nodeContentItemReader,
            textController);
        CreateNodesTool createNodesTool = new CreateNodesTool(availableMaps, nodeModelCreator, nodeInserter,
            modifiedNodeSummaryBuilder, nodeContentApplier);
        MoveNodesTool moveNodesTool = new MoveNodesTool(availableMaps, mapController, anchorPlacementCalculator);
        CreateSummaryTool createSummaryTool = new CreateSummaryTool(availableMaps, nodeModelCreator, nodeInserter,
            summaryNodeCreator, modifiedNodeSummaryBuilder, nodeContentApplier);
        MoveNodesIntoSummaryTool moveNodesIntoSummaryTool = new MoveNodesIntoSummaryTool(availableMaps, mapController,
            summaryNodeCreator);
        this.systemMessageBuilder = Objects.requireNonNull(systemMessageBuilder, "systemMessageBuilder");
        this.readNodeWithContextTool = Objects.requireNonNull(readNodeWithContextTool, "readNodeWithContextTool");
        this.selectedMapAndNodeIdentifiersTool = Objects.requireNonNull(
            selectedMapAndNodeIdentifiersTool, "selectedMapAndNodeIdentifiersTool");
        this.searchNodesTool = Objects.requireNonNull(searchNodesTool, "searchNodesTool");
        this.createNodesTool = Objects.requireNonNull(createNodesTool, "createNodesTool");
        this.moveNodesTool = Objects.requireNonNull(moveNodesTool, "moveNodesTool");
        this.createSummaryTool = Objects.requireNonNull(createSummaryTool, "createSummaryTool");
        this.moveNodesIntoSummaryTool = Objects.requireNonNull(moveNodesIntoSummaryTool, "moveNodesIntoSummaryTool");
        this.toolCallSummaryHandler = toolCallSummaryHandler;
    }

    public String systemMessageForChat(Object input) {
        return systemMessageBuilder.buildForChat();
    }

    @Tool("Read nodes with context.")
    public ReadNodesWithContextResponse readNodeWithContext(ReadNodesWithContextRequest request) {
        try {
            ReadNodesWithContextResponse response = readNodeWithContextTool.readNodeWithContext(request);
            publishToolCallSummary(readNodeWithContextTool.buildToolCallSummary(request, response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(readNodeWithContextTool.buildToolCallErrorSummary(request, error));
            throw error;
        }
    }

    @Tool("Get identifiers for the currently selected map and node.")
    public SelectionIdentifiersResponse getSelectedMapAndNodeIdentifiers() {
        try {
            SelectionIdentifiersResponse response = selectedMapAndNodeIdentifiersTool.getSelectedMapAndNodeIdentifiers();
            publishToolCallSummary(selectedMapAndNodeIdentifiersTool.buildToolCallSummary(response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(selectedMapAndNodeIdentifiersTool.buildToolCallErrorSummary(error));
            throw error;
        }
    }

    @Tool("Search nodes by content.")
    public SearchNodesResponse searchNodes(SearchNodesRequest request) {
        try {
            SearchNodesResponse response = searchNodesTool.searchNodes(request);
            publishToolCallSummary(searchNodesTool.buildToolCallSummary(request, response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(searchNodesTool.buildToolCallErrorSummary(request, error));
            throw error;
        }
    }

    private void publishToolCallSummary(ToolCallSummary summary) {
        if (summary == null) {
            return;
        }
        LogUtils.info(summary.getSummaryText());
        if (toolCallSummaryHandler != null) {
            toolCallSummaryHandler.handleToolCallSummary(summary);
        }
    }

    // @Tool("Return a flat list of nodes under a branch.")
    public FlatListResponse getFlatList(FlatListRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("List properties that can be used for search and filter conditions, excluding attributes.")
    public SearchPropertiesResponse listSearchProperties(SearchPropertiesRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("List valid conditions and value input modes for a property.")
    public SearchConditionsForPropertyResponse listSearchConditionsForProperty(
            SearchConditionsForPropertyRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("Search nodes using a condition based on search and filter properties.")
    public SearchNodesByConditionResponse searchNodesByCondition(SearchNodesByConditionRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("List attribute names available for a map.")
    public AttributeNamesForMapResponse listAttributeNamesForMap(AttributeNamesForMapRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("Search nodes by attribute name and value.")
    public SearchAttributesByNameAndValueResponse searchAttributesByNameAndValue(
            SearchAttributesByNameAndValueRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("Generate a compact overview and index for targeted search.")
    public SearchOverviewResponse generateSearchOverview(SearchOverviewRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("Set an AI only filter condition that affects only AI tool calls.")
    public SetAiOnlyFilterConditionResponse setAiOnlyFilterCondition(SetAiOnlyFilterConditionRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("Get the active AI only filter condition.")
    public GetAiOnlyFilterConditionResponse getAiOnlyFilterCondition(GetAiOnlyFilterConditionRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("Clear the active AI only filter condition.")
    public ClearAiOnlyFilterConditionResponse clearAiOnlyFilterCondition(ClearAiOnlyFilterConditionRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Create nodes and subtrees relative to an anchor node. Omit optional textual fields such as details and note when they are empty instead of sending empty strings so the tool leaves those values untouched.")
    public CreateNodesResponse createNodes(CreateNodesRequest request) {
        try {
            CreateNodesResponse response = createNodesTool.createNodes(request);
            publishToolCallSummary(createNodesTool.buildToolCallSummary(request, response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(createNodesTool.buildToolCallErrorSummary(request, error));
            throw error;
        }
    }

    // @Tool("Apply attributes to selected nodes.")
    public ApplyAttributesResponse applyAttributes(ApplyAttributesRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Tool("Move nodes relative to an anchor node.")
    public MoveNodesResponse moveNodes(MoveNodesRequest request) {
        try {
            MoveNodesResponse response = moveNodesTool.moveNodes(request);
            publishToolCallSummary(moveNodesTool.buildToolCallSummary(request, response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(moveNodesTool.buildToolCallErrorSummary(request, error));
            throw error;
        }
    }

    @Tool("Create summary content and a summary bracket for a summarized range. Omit optional textual fields such as details and note when they are empty instead of sending empty strings so the tool leaves those values untouched.")
    public CreateSummaryResponse createSummary(CreateSummaryRequest request) {
        try {
            CreateSummaryResponse response = createSummaryTool.createSummary(request);
            publishToolCallSummary(createSummaryTool.buildToolCallSummary(request, response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(createSummaryTool.buildToolCallErrorSummary(request, error));
            throw error;
        }
    }

    @Tool("Move existing nodes to become summary content for a summarized range.")
    public MoveNodesIntoSummaryResponse moveNodesIntoSummary(MoveNodesIntoSummaryRequest request) {
        try {
            MoveNodesIntoSummaryResponse response = moveNodesIntoSummaryTool.moveNodesIntoSummary(request);
            publishToolCallSummary(moveNodesIntoSummaryTool.buildToolCallSummary(request, response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(moveNodesIntoSummaryTool.buildToolCallErrorSummary(request, error));
            throw error;
        }
    }
}
