package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import dev.langchain4j.agent.tool.Tool;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.text.TextController;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.plugin.ai.chat.SystemMessageBuilder;
import org.freeplane.plugin.ai.maps.AvailableMaps;

public class AIToolSet {
    private final SystemMessageBuilder systemMessageBuilder;
    private final ReadNodesWithDescendantsTool readNodesWithDescendantsTool;
    private final SelectedMapAndNodeIdentifiersTool selectedMapAndNodeIdentifiersTool;
    private final SelectSingleNodeTool selectSingleNodeTool;
    private final SearchNodesTool searchNodesTool;
    private final CreateNodesTool createNodesTool;
    private final MoveNodesTool moveNodesTool;
    private final CreateSummaryTool createSummaryTool;
    private final MoveNodesIntoSummaryTool moveNodesIntoSummaryTool;
    private final ListAvailableIconsTool listAvailableIconsTool;
    private final NodeContentEditor nodeContentEditor;
    private final AvailableMaps availableMaps;
    private final ToolCallSummaryHandler toolCallSummaryHandler;
    private final ToolCaller toolCaller;

    AIToolSet(ToolCallSummaryHandler toolCallSummaryHandler, AvailableMaps availableMaps, TextController textController,
              NodeContentFactories nodeContentFactories, MMapController mapController,
              ToolCaller toolCaller) {
        Objects.requireNonNull(mapController, "mapController");
        this.availableMaps = Objects.requireNonNull(availableMaps, "availableMaps");
        NodeModelCreator nodeModelCreator = new NodeModelCreator();
        AnchorPlacementCalculator anchorPlacementCalculator = new AnchorPlacementCalculator();
        NodeInserter nodeInserter = new NodeInserter(mapController, anchorPlacementCalculator);
        SummaryNodeCreator summaryNodeCreator = new SummaryNodeCreator(mapController);
        ModifiedNodeSummaryBuilder modifiedNodeSummaryBuilder = new ModifiedNodeSummaryBuilder(textController);
        TextContentWriteController textContentWriteController = new TextContentWriteControllerAdapter(
            MTextController.getController());
        NoteContentWriteController noteContentWriteController = new NoteContentWriteControllerAdapter(
            MNoteController.getController());
        MAttributeController attributeController = MAttributeController.getController();
        MIconController iconController = (MIconController) IconController.getController();
        TextualContentEditor textualContentEditor = new TextualContentEditor(
            textContentWriteController, noteContentWriteController);
        AttributesContentEditor attributesContentEditor = new AttributesContentEditor(attributeController);
        TagsContentEditor tagsContentEditor = new TagsContentEditor(iconController);
        List<NamedIcon> iconCandidates = new ArrayList<>(IconStoreFactory.ICON_STORE.getMindIcons());
        iconCandidates.addAll(IconStoreFactory.ICON_STORE.getUserIcons());
        IconsContentEditor iconsContentEditor = new IconsContentEditor(
            nodeContentFactories.iconDescriptionResolver, iconCandidates, iconController);
        NodeContentApplier nodeContentApplier = new NodeContentApplier(textualContentEditor, attributesContentEditor,
            tagsContentEditor, iconsContentEditor);
        NodeCreationHierarchyBuilder nodeCreationHierarchyBuilder = new NodeCreationHierarchyBuilder(
            nodeModelCreator, nodeContentApplier);
        NodeContentEditor nodeContentEditor = new NodeContentEditor(textController, nodeContentFactories.nodeContentItemReader,
            textualContentEditor, attributesContentEditor, tagsContentEditor, iconsContentEditor);
        SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
        ReadNodesWithDescendantsTool readNodesWithDescendantsTool = new ReadNodesWithDescendantsTool(
            availableMaps, nodeContentFactories.nodeContentItemReader, textController);
        SelectedMapAndNodeIdentifiersTool selectedMapAndNodeIdentifiersTool = new SelectedMapAndNodeIdentifiersTool(
            availableMaps, textController);
        SelectSingleNodeTool selectSingleNodeTool = new SelectSingleNodeTool(
            availableMaps, mapController, selectedMapAndNodeIdentifiersTool);
        SearchNodesTool searchNodesTool = new SearchNodesTool(availableMaps, nodeContentFactories.nodeContentItemReader,
            textController);
        CreateNodesTool createNodesTool = new CreateNodesTool(availableMaps, nodeCreationHierarchyBuilder, nodeInserter,
            modifiedNodeSummaryBuilder);
        MoveNodesTool moveNodesTool = new MoveNodesTool(availableMaps, mapController, anchorPlacementCalculator);
        CreateSummaryTool createSummaryTool = new CreateSummaryTool(availableMaps, nodeCreationHierarchyBuilder,
            nodeInserter, summaryNodeCreator, modifiedNodeSummaryBuilder);
        MoveNodesIntoSummaryTool moveNodesIntoSummaryTool = new MoveNodesIntoSummaryTool(availableMaps, mapController,
            summaryNodeCreator);
        ListAvailableIconsTool listAvailableIconsTool = new ListAvailableIconsTool(
            nodeContentFactories.iconDescriptionResolver);
        this.systemMessageBuilder = Objects.requireNonNull(systemMessageBuilder, "systemMessageBuilder");
        this.readNodesWithDescendantsTool = Objects.requireNonNull(readNodesWithDescendantsTool,
            "readNodesWithDescendantsTool");
        this.selectedMapAndNodeIdentifiersTool = Objects.requireNonNull(
            selectedMapAndNodeIdentifiersTool, "selectedMapAndNodeIdentifiersTool");
        this.selectSingleNodeTool = Objects.requireNonNull(selectSingleNodeTool, "selectSingleNodeTool");
        this.searchNodesTool = Objects.requireNonNull(searchNodesTool, "searchNodesTool");
        this.createNodesTool = Objects.requireNonNull(createNodesTool, "createNodesTool");
        this.moveNodesTool = Objects.requireNonNull(moveNodesTool, "moveNodesTool");
        this.createSummaryTool = Objects.requireNonNull(createSummaryTool, "createSummaryTool");
        this.moveNodesIntoSummaryTool = Objects.requireNonNull(moveNodesIntoSummaryTool, "moveNodesIntoSummaryTool");
        this.listAvailableIconsTool = Objects.requireNonNull(listAvailableIconsTool, "listAvailableIconsTool");
        this.nodeContentEditor = Objects.requireNonNull(nodeContentEditor, "nodeContentEditor");
        this.toolCallSummaryHandler = toolCallSummaryHandler;
        this.toolCaller = toolCaller == null
            ? ToolCaller.CHAT
            : toolCaller;
    }

    public String systemMessageForChat(Object input) {
        return systemMessageBuilder.buildForChat();
    }

    @Tool("Read nodes with descendants.")
    public ReadNodesWithDescendantsResponse readNodesWithDescendants(ReadNodesWithDescendantsRequest request) {
        try {
            ReadNodesWithDescendantsResponse response = readNodesWithDescendantsTool.readNodesWithDescendants(request);
            publishToolCallSummary(readNodesWithDescendantsTool.buildToolCallSummary(request, response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(readNodesWithDescendantsTool.buildToolCallErrorSummary(request, error));
            throw error;
        }
    }

    @Tool("Fetch nodes for editing. Returns editable content only.")
    public FetchNodesForEditingResponse fetchNodesForEditing(FetchNodesForEditingRequest request) {
        try {
            FetchNodesForEditingResponse response = readNodesWithDescendantsTool.fetchNodesForEditing(request);
            publishToolCallSummary(readNodesWithDescendantsTool.buildFetchToolCallSummary(request, response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(readNodesWithDescendantsTool.buildFetchToolCallErrorSummary(request, error));
            throw error;
        }
    }

    @Tool("Get identifiers for the currently selected map and node, with configurable selection collection mode.")
    public SelectionIdentifiersResponse getSelectedMapAndNodeIdentifiers(SelectionIdentifiersRequest request) {
        try {
            SelectionIdentifiersResponse response = selectedMapAndNodeIdentifiersTool.getSelectedMapAndNodeIdentifiers(request);
            publishToolCallSummary(selectedMapAndNodeIdentifiersTool.buildToolCallSummary(response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(selectedMapAndNodeIdentifiersTool.buildToolCallErrorSummary(error));
            throw error;
        }
    }

    @Tool("Select a single node by identifier and make it visible in the current view. This updates the node shown "
        + "to the user in the UI and should be used only for user communication; unexpected use can disrupt user "
        + "workflow and reduce AI acceptance.")
    public SelectionIdentifiersResponse selectSingleNode(SelectSingleNodeRequest request) {
        try {
            SelectionIdentifiersResponse response = selectSingleNodeTool.selectSingleNode(request);
            publishToolCallSummary(selectSingleNodeTool.buildToolCallSummary(response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(selectSingleNodeTool.buildToolCallErrorSummary(request, error));
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

    @Tool("List available built-in and user-defined icons. Emoji icons are referenced by the emoji character itself "
        + "and are not listed here.")
    public ListAvailableIconsResponse listAvailableIcons() {
        try {
            ListAvailableIconsResponse response = listAvailableIconsTool.listAvailableIcons();
            publishToolCallSummary(listAvailableIconsTool.buildToolCallSummary(response));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(listAvailableIconsTool.buildToolCallErrorSummary(error));
            throw error;
        }
    }

    @Tool("Edit node content safely through undo-aware controllers.\n"
        + "IMPORTANT RULE: Before editing, you must call fetchNodesForEditing tool to get the real edited node content "
        + "and content type.\n "
        + "Formatting: use HTML unless originalContentType is MARKDOWN or LATEX")
    public List<NodeContentItem> edit(EditRequest request) {
        try {
            List<NodeContentItem> response = editNodes(request);
            publishToolCallSummary(buildEditToolSummary(request, response, false, null));
            return response;
        } catch (RuntimeException error) {
            publishToolCallSummary(buildEditToolSummary(request, null, true, error.getMessage()));
            throw error;
        }
    }

    private void publishToolCallSummary(ToolCallSummary summary) {
        if (summary == null) {
            return;
        }
        LogUtils.info(summary.getSummaryText());
        if (toolCallSummaryHandler != null) {
            toolCallSummaryHandler.handleToolCallSummary(
                summary.withToolCaller(toolCaller));
        }
    }

    private List<NodeContentItem> editNodes(EditRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Missing request");
        }
        String mapIdentifierValue = requireValue(request.getMapIdentifier(), "mapIdentifier");
        UUID mapIdentifier = parseMapIdentifier(mapIdentifierValue);
        MapModel mapModel = availableMaps.findMapModel(mapIdentifier);
        if (mapModel == null) {
            throw new IllegalArgumentException("Unknown map identifier: " + mapIdentifierValue);
        }
        List<NodeContentEditItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Missing edit items");
        }
        Map<String, List<NodeContentEditItem>> itemsByNode = new LinkedHashMap<>();
        for (NodeContentEditItem item : items) {
            if (item == null) {
                continue;
            }
            String nodeIdentifier = requireValue(item.getNodeIdentifier(), "nodeIdentifier");
            itemsByNode.computeIfAbsent(nodeIdentifier, key -> new ArrayList<>()).add(item);
        }
        List<NodeContentItem> results = new ArrayList<>(itemsByNode.size());
        List<String> unknownNodeIdentifiers = new ArrayList<>();
        for (Map.Entry<String, List<NodeContentEditItem>> entry : itemsByNode.entrySet()) {
            String nodeIdentifier = entry.getKey();
            NodeModel nodeModel = mapModel.getNodeForID(nodeIdentifier);
            if (nodeModel == null) {
                unknownNodeIdentifiers.add(nodeIdentifier);
                continue;
            }
            results.add(nodeContentEditor.edit(nodeModel, entry.getValue()));
        }
        if (!unknownNodeIdentifiers.isEmpty()) {
            throw new IllegalArgumentException("Invalid node identifiers: " + String.join(", ", unknownNodeIdentifiers));
        }
        return results;
    }

    private String requireValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + fieldName + ".");
        }
        return value;
    }

    private UUID parseMapIdentifier(String mapIdentifier) {
        try {
            return UUID.fromString(mapIdentifier);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid map identifier: " + mapIdentifier);
        }
    }

    private ToolCallSummary buildEditToolSummary(EditRequest request, List<NodeContentItem> response,
                                                 boolean hasError, String errorMessage) {
        if (request == null) {
            return null;
        }
        int itemCount = request.getItems() == null ? 0 : request.getItems().size();
        int nodeCount = response == null ? 0 : response.size();
        String summaryText = "edit: nodes=" + nodeCount + ", items=" + itemCount;
        if (request.getUserSummary() != null && !request.getUserSummary().isEmpty()) {
            summaryText = summaryText + ", userSummary=\"" + request.getUserSummary() + "\"";
        }
        if (hasError) {
            String safeMessage = ToolCallSummaryFormatter.sanitizeValue(errorMessage);
            if (!safeMessage.isEmpty()) {
                summaryText = summaryText + ", error=\"" + safeMessage + "\"";
            } else {
                summaryText = summaryText + ", error=true";
            }
        }
        return new ToolCallSummary("edit", summaryText, hasError);
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
