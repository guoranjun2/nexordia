package org.freeplane.plugin.ai.tools;

import java.util.Objects;

import dev.langchain4j.agent.tool.Tool;
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
    private final ReadNodeContentTool readNodeContentTool;
    private final BreadcrumbsTool breadcrumbsTool;

    public AIToolSet() {
        this(createAvailableMaps(), createTextController(), createAttributeController(), createIconController());
    }

    AIToolSet(AvailableMaps availableMaps, TextController textController,
              AttributeController attributeController, IconController iconController) {
        this(availableMaps, createNodeContentItemReader(textController, attributeController, iconController));
    }

    AIToolSet(AvailableMaps availableMaps, NodeContentItemReader nodeContentItemReader) {
        this(new SystemMessageBuilder(availableMaps),
            new ReadNodeContentTool(availableMaps, nodeContentItemReader),
            new BreadcrumbsTool(availableMaps, nodeContentItemReader));
    }

    AIToolSet(SystemMessageBuilder systemMessageBuilder, ReadNodeContentTool readNodeContentTool,
              BreadcrumbsTool breadcrumbsTool) {
        this.systemMessageBuilder = Objects.requireNonNull(systemMessageBuilder, "systemMessageBuilder");
        this.readNodeContentTool = Objects.requireNonNull(readNodeContentTool, "readNodeContentTool");
        this.breadcrumbsTool = Objects.requireNonNull(breadcrumbsTool, "breadcrumbsTool");
    }

    public String systemMessageForChat(Object input) {
        return systemMessageBuilder.buildForChat();
    }

    @Tool("Read node content with parent and child context.")
    public ReadNodeContentResponse readNodeContent(ReadNodeContentRequest request) {
        return readNodeContentTool.readNodeContent(request);
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
        NodeContentReader nodeContentReader = new NodeContentReader(
            textualContentReader, attributesContentReader, tagsContentReader);
        return new NodeContentItemReader(nodeContentReader);
    }

    @Tool("Get breadcrumbs from the root to a node.")
    public BreadcrumbsResponse getBreadcrumbs(BreadcrumbsRequest request) {
        return breadcrumbsTool.getBreadcrumbs(request);
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
