package org.freeplane.plugin.ai.tools;

import java.util.Objects;

import dev.langchain4j.agent.tool.Tool;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.chat.SystemMessageBuilder;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.freeplane.plugin.ai.maps.ControllerMapModelProvider;

public class AIToolSet {
    private final SystemMessageBuilder systemMessageBuilder;
    private final ReadNodeWithContextTool readNodeWithContextTool;
    private final SelectedMapAndNodeIdentifiersTool selectedMapAndNodeIdentifiersTool;
    private final SearchNodesTool searchNodesTool;
    private final ToolCallSummaryHandler toolCallSummaryHandler;

    public AIToolSet() {
        this(null);
    }

    public AIToolSet(ToolCallSummaryHandler toolCallSummaryHandler) {
        this(toolCallSummaryHandler, createAvailableMaps(), createTextController(), createAttributeController(),
            createIconController());
    }

    AIToolSet(ToolCallSummaryHandler toolCallSummaryHandler, AvailableMaps availableMaps, TextController textController,
              AttributeController attributeController, IconController iconController) {
        this(toolCallSummaryHandler, availableMaps,
            createNodeContentItemReader(textController, attributeController, iconController), textController);
    }

    AIToolSet(ToolCallSummaryHandler toolCallSummaryHandler, AvailableMaps availableMaps,
              NodeContentItemReader nodeContentItemReader, TextController textController) {
        this(new SystemMessageBuilder(),
            new ReadNodeWithContextTool(availableMaps, nodeContentItemReader, textController),
            new SelectedMapAndNodeIdentifiersTool(availableMaps),
            new SearchNodesTool(availableMaps, nodeContentItemReader, textController),
            toolCallSummaryHandler);
    }

    AIToolSet(SystemMessageBuilder systemMessageBuilder, ReadNodeWithContextTool readNodeWithContextTool,
              SelectedMapAndNodeIdentifiersTool selectedMapAndNodeIdentifiersTool,
              SearchNodesTool searchNodesTool, ToolCallSummaryHandler toolCallSummaryHandler) {
        this.systemMessageBuilder = Objects.requireNonNull(systemMessageBuilder, "systemMessageBuilder");
        this.readNodeWithContextTool = Objects.requireNonNull(readNodeWithContextTool, "readNodeWithContextTool");
        this.selectedMapAndNodeIdentifiersTool = Objects.requireNonNull(
            selectedMapAndNodeIdentifiersTool, "selectedMapAndNodeIdentifiersTool");
        this.searchNodesTool = Objects.requireNonNull(searchNodesTool, "searchNodesTool");
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

    private static AvailableMaps createAvailableMaps() {
        return new AvailableMaps(new ControllerMapModelProvider());
    }

    private static TextController createTextController() {
        ModeController modeController = requireModeController();
        TextController textController = modeController.getExtension(TextController.class);
        if (textController == null) {
            throw new IllegalStateException("Text controller is not available.");
        }
        return textController;
    }

    private static AttributeController createAttributeController() {
        ModeController modeController = requireModeController();
        AttributeController attributeController = modeController.getExtension(AttributeController.class);
        if (attributeController == null) {
            throw new IllegalStateException("Attribute controller is not available.");
        }
        return attributeController;
    }

    private static IconController createIconController() {
        ModeController modeController = requireModeController();
        IconController iconController = modeController.getExtension(IconController.class);
        if (iconController == null) {
            throw new IllegalStateException("Icon controller is not available.");
        }
        return iconController;
    }

    private static ModeController requireModeController() {
        ModeController modeController = Controller.getCurrentModeController();
        if (modeController == null) {
            throw new IllegalStateException("Current mode controller is not available.");
        }
        return modeController;
    }

    private static NodeContentItemReader createNodeContentItemReader(TextController textController,
                                                                     AttributeController attributeController,
                                                                     IconController iconController) {
        TextualContentReader textualContentReader = new TextualContentReader(textController);
        AttributesContentReader attributesContentReader = new AttributesContentReader(attributeController, textController);
        TagsContentReader tagsContentReader = new TagsContentReader(iconController);
        EnglishTextProvider englishTextProvider = new DefaultEnglishTextProvider();
        IconsContentReader iconsContentReader = new IconsContentReader(englishTextProvider, iconController);
        NodeContentReader nodeContentReader = new NodeContentReader(
            textualContentReader, attributesContentReader, tagsContentReader, iconsContentReader);
        return new NodeContentItemReader(nodeContentReader);
    }

    private void publishToolCallSummary(ToolCallSummary summary) {
        if (summary == null) {
            return;
        }
        if (summary.hasError()) {
            LogUtils.severe(summary.getSummaryText());
        } else {
            LogUtils.info(summary.getSummaryText());
        }
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

    // @Tool("Create nodes and subtrees under a target parent.")
    public CreateNodesResponse createNodes(CreateNodesRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("Apply attributes to selected nodes.")
    public ApplyAttributesResponse applyAttributes(ApplyAttributesRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // @Tool("Move nodes under a new parent.")
    public MoveNodesResponse moveNodes(MoveNodesRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
