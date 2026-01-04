package org.freeplane.plugin.ai.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.SummaryNode;
import org.freeplane.plugin.ai.maps.AvailableMaps;

public class ReadNodeWithContextTool {
    private static final EnumSet<ContextSection> DEFAULT_SECTIONS = EnumSet.of(ContextSection.BREADCRUMB_PATH);
    private static final int DEFAULT_FULL_CONTENT_DEPTH = 0;
    private static final int DEFAULT_SUMMARY_DEPTH = 1;
    private static final int DEFAULT_MAXIMUM_TOTAL_TEXT_CHARACTERS = 65536;

    private final AvailableMaps availableMaps;
    private final NodeContentItemReader nodeContentItemReader;
    private final ObjectMapper objectMapper;

    public ReadNodeWithContextTool(AvailableMaps availableMaps, NodeContentItemReader nodeContentItemReader) {
        this(availableMaps, nodeContentItemReader, new ObjectMapper());
    }

    ReadNodeWithContextTool(AvailableMaps availableMaps, NodeContentItemReader nodeContentItemReader,
                            ObjectMapper objectMapper) {
        this.availableMaps = Objects.requireNonNull(availableMaps, "availableMaps");
        this.nodeContentItemReader = Objects.requireNonNull(nodeContentItemReader, "nodeContentItemReader");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public ReadNodesWithContextResponse readNodeWithContext(ReadNodesWithContextRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Missing request");
        }
        String mapIdentifierValue = requireValue(request.getMapIdentifier(), "mapIdentifier");
        UUID mapIdentifier = parseMapIdentifier(mapIdentifierValue);
        MapModel mapModel = availableMaps.findMapModel(mapIdentifier);
        if (mapModel == null) {
            throw new IllegalArgumentException("Unknown map identifier: " + mapIdentifierValue);
        }
        List<String> nodeIdentifiers = resolveNodeIdentifiers(mapModel, request.getNodeIdentifiers());
        validateDuplicateNodeIdentifiers(nodeIdentifiers);
        List<NodeModel> focusNodes = resolveFocusNodes(mapModel, nodeIdentifiers);
        EnumSet<ContextSection> sections = resolveSections(request.getContextSections());
        boolean includeQualifiers = sections.contains(ContextSection.QUALIFIERS);
        int fullContentDepth = request.getFullContentDepth() == null
            ? DEFAULT_FULL_CONTENT_DEPTH
            : request.getFullContentDepth();
        int summaryDepth = request.getSummaryDepth() == null
            ? DEFAULT_SUMMARY_DEPTH
            : request.getSummaryDepth();
        if (fullContentDepth < 0 || summaryDepth < 0) {
            throw new IllegalArgumentException("Depth values must be 0 or greater");
        }
        int maximumTotalTextCharacters = request.getMaximumTotalTextCharacters() == null
            ? DEFAULT_MAXIMUM_TOTAL_TEXT_CHARACTERS
            : request.getMaximumTotalTextCharacters();
        boolean enforceBudget = focusNodes.size() > 1;
        NodeContentRequest focusNodeContentRequest = request.getFocusNodeContentRequest();
        NodeContentRequest parentNodeContentRequest = request.getParentNodeContentRequest();
        NodeContentRequest childNodeContentRequest = request.getChildNodeContentRequest();
        List<ReadNodesWithContextItem> items = new ArrayList<>();
        int budgetUsed = 0;
        int omittedFocusNodeCount = 0;
        int omittedChildCount = 0;
        int omittedDescendantCount = 0;
        for (NodeModel focusNode : focusNodes) {
            ReadNodesWithContextItem item = buildItemForFocusNode(
                focusNode,
                sections,
                fullContentDepth,
                summaryDepth,
                includeQualifiers,
                focusNodeContentRequest,
                parentNodeContentRequest,
                childNodeContentRequest,
                enforceBudget,
                maximumTotalTextCharacters,
                budgetUsed);
            if (item == null) {
                omittedFocusNodeCount = focusNodes.size() - items.size();
                break;
            }
            int itemSize = measureSerializedLength(item);
            if (enforceBudget) {
                if (budgetUsed + itemSize > maximumTotalTextCharacters) {
                    omittedFocusNodeCount = focusNodes.size() - items.size();
                    break;
                }
                budgetUsed += itemSize;
            }
            Omissions omissions = item.getOmissions();
            if (omissions != null) {
                omittedChildCount += safeCount(omissions.getOmittedChildCount());
                omittedDescendantCount += safeCount(omissions.getOmittedDescendantCount());
            }
            items.add(item);
        }
        Omissions responseOmissions = buildResponseOmissions(
            omittedFocusNodeCount, omittedChildCount, omittedDescendantCount);
        if (responseOmissions != null && !items.isEmpty()) {
            ReadNodesWithContextItem lastItem = items.get(items.size() - 1);
            Omissions merged = mergeOmissions(lastItem.getOmissions(), responseOmissions);
            items.set(items.size() - 1, new ReadNodesWithContextItem(
                lastItem.getNodes(), lastItem.getParentNode(), lastItem.getBreadcrumbPath(), merged));
        }
        return new ReadNodesWithContextResponse(mapIdentifierValue, items);
    }

    private ReadNodesWithContextItem buildItemForFocusNode(NodeModel focusNode,
                                                           EnumSet<ContextSection> sections,
                                                           int fullContentDepth,
                                                           int summaryDepth,
                                                           boolean includeQualifiers,
                                                           NodeContentRequest focusNodeContentRequest,
                                                           NodeContentRequest parentNodeContentRequest,
                                                           NodeContentRequest childNodeContentRequest,
                                                           boolean enforceBudget,
                                                           int maximumTotalTextCharacters,
                                                           int budgetUsed) {
        List<NodeDepthItem> allNodes = buildNodeDepthItems(
            focusNode,
            fullContentDepth,
            summaryDepth,
            includeQualifiers,
            focusNodeContentRequest,
            childNodeContentRequest);
        if (allNodes.isEmpty()) {
            return null;
        }
        NodeContentItem parentNode = buildParentNodeItem(focusNode, sections, includeQualifiers, parentNodeContentRequest);
        String breadcrumbPath = sections.contains(ContextSection.BREADCRUMB_PATH) ? buildBreadcrumbPath(focusNode) : null;
        List<NodeDepthItem> nodes = new ArrayList<>();
        ReadNodesWithContextItem baseItem = new ReadNodesWithContextItem(nodes, parentNode, breadcrumbPath, null);
        int omittedChildCount = 0;
        int omittedDescendantCount = 0;
        for (int index = 0; index < allNodes.size(); index += 1) {
            NodeDepthItem nodeDepthItem = allNodes.get(index);
            nodes.add(nodeDepthItem);
            int itemSize = measureSerializedLength(baseItem);
            if (enforceBudget && budgetUsed + itemSize > maximumTotalTextCharacters) {
                nodes.remove(nodes.size() - 1);
                if (nodes.isEmpty()) {
                    return null;
                }
                for (int remaining = index; remaining < allNodes.size(); remaining += 1) {
                    NodeDepthItem omittedNode = allNodes.get(remaining);
                    if (omittedNode.getDepth() == 1) {
                        omittedChildCount += 1;
                    } else if (omittedNode.getDepth() > 1) {
                        omittedDescendantCount += 1;
                    }
                }
                break;
            }
        }
        Omissions omissions = omittedChildCount > 0 || omittedDescendantCount > 0
            ? new Omissions(null, omittedChildCount, omittedDescendantCount, null,
                Collections.singletonList(OmissionReason.TEXT_BUDGET))
            : null;
        return new ReadNodesWithContextItem(nodes, parentNode, breadcrumbPath, omissions);
    }

    private List<NodeDepthItem> buildNodeDepthItems(NodeModel focusNode,
                                                    int fullContentDepth,
                                                    int summaryDepth,
                                                    boolean includeQualifiers,
                                                    NodeContentRequest focusNodeContentRequest,
                                                    NodeContentRequest childNodeContentRequest) {
        int maximumDepth = fullContentDepth + summaryDepth;
        List<NodeDepthItem> nodes = new ArrayList<>();
        Deque<NodeModel> stack = new ArrayDeque<>();
        Deque<Integer> depthStack = new ArrayDeque<>();
        stack.push(focusNode);
        depthStack.push(0);
        while (!stack.isEmpty()) {
            NodeModel current = stack.pop();
            int depth = depthStack.pop();
            if (depth > maximumDepth) {
                continue;
            }
            NodeDepthItem nodeDepthItem = buildNodeDepthItem(
                current, depth, fullContentDepth, includeQualifiers, focusNodeContentRequest, childNodeContentRequest);
            nodes.add(nodeDepthItem);
            if (depth < maximumDepth) {
                List<NodeModel> children = current.getChildren();
                for (int index = children.size() - 1; index >= 0; index -= 1) {
                    stack.push(children.get(index));
                    depthStack.push(depth + 1);
                }
            }
        }
        return nodes;
    }

    private NodeDepthItem buildNodeDepthItem(NodeModel nodeModel, int depth, int fullContentDepth,
                                             boolean includeQualifiers,
                                             NodeContentRequest focusNodeContentRequest,
                                             NodeContentRequest childNodeContentRequest) {
        NodeContent content;
        if (depth == 0) {
            content = nodeContentItemReader.readNodeContent(nodeModel, focusNodeContentRequest, NodeContentPreset.FULL);
        } else if (depth <= fullContentDepth) {
            content = nodeContentItemReader.readNodeContent(nodeModel, childNodeContentRequest, NodeContentPreset.BRIEF);
        } else {
            content = nodeContentItemReader.readNodeContent(nodeModel, null, NodeContentPreset.BRIEF);
        }
        if (depth > fullContentDepth) {
            content = nodeContentItemReader.readNodeContent(nodeModel, null, NodeContentPreset.BRIEF);
        }
        List<String> qualifiers = includeQualifiers ? buildQualifiers(nodeModel) : null;
        return new NodeDepthItem(nodeModel.createID(), depth, content, qualifiers);
    }

    private NodeContentItem buildParentNodeItem(NodeModel focusNode, EnumSet<ContextSection> sections,
                                                boolean includeQualifiers, NodeContentRequest parentNodeContentRequest) {
        if (!sections.contains(ContextSection.PARENT_SUMMARY)) {
            return null;
        }
        NodeModel parentNode = focusNode.getParentNode();
        if (parentNode == null) {
            return null;
        }
        NodeContent content = nodeContentItemReader.readNodeContent(
            parentNode, parentNodeContentRequest, NodeContentPreset.BRIEF);
        return nodeContentItemReader.readNodeContentItem(parentNode, content, true, includeQualifiers);
    }

    private String buildBreadcrumbPath(NodeModel nodeModel) {
        List<String> pathSegments = new ArrayList<>();
        NodeModel current = nodeModel;
        while (current != null) {
            if (!SummaryNode.isHidden(current)) {
                NodeContent briefContent = nodeContentItemReader.readNodeContent(current, null, NodeContentPreset.BRIEF);
                String text = briefContent == null ? null : briefContent.getBriefText();
                if (text != null && !text.isEmpty()) {
                    pathSegments.add(text);
                }
            }
            current = current.getParentNode();
        }
        if (pathSegments.isEmpty()) {
            return null;
        }
        Collections.reverse(pathSegments);
        return String.join("/", pathSegments);
    }

    private List<String> resolveNodeIdentifiers(MapModel mapModel, List<String> nodeIdentifiers) {
        if (nodeIdentifiers != null && !nodeIdentifiers.isEmpty()) {
            return nodeIdentifiers;
        }
        NodeModel rootNode = mapModel.getRootNode();
        if (rootNode == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(rootNode.getID());
    }

    private void validateDuplicateNodeIdentifiers(List<String> nodeIdentifiers) {
        Set<String> seen = new LinkedHashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (String nodeIdentifier : nodeIdentifiers) {
            if (!seen.add(nodeIdentifier)) {
                duplicates.add(nodeIdentifier);
            }
        }
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("duplicate node identifiers");
        }
    }

    private List<NodeModel> resolveFocusNodes(MapModel mapModel, List<String> nodeIdentifiers) {
        List<String> unknown = new ArrayList<>();
        List<NodeModel> focusNodes = new ArrayList<>(nodeIdentifiers.size());
        for (String nodeIdentifier : nodeIdentifiers) {
            NodeModel node = mapModel.getNodeForID(nodeIdentifier);
            if (node == null) {
                unknown.add(nodeIdentifier);
            } else {
                focusNodes.add(node);
            }
        }
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown node identifiers: " + String.join(", ", unknown));
        }
        return focusNodes;
    }

    private EnumSet<ContextSection> resolveSections(List<ContextSection> contextSections) {
        if (contextSections == null || contextSections.isEmpty()) {
            return DEFAULT_SECTIONS.clone();
        }
        EnumSet<ContextSection> sections = EnumSet.noneOf(ContextSection.class);
        for (ContextSection section : contextSections) {
            if (section != null) {
                sections.add(section);
            }
        }
        if (sections.isEmpty()) {
            return DEFAULT_SECTIONS.clone();
        }
        return sections;
    }

    private Omissions buildResponseOmissions(int omittedFocusNodeCount, int omittedChildCount,
                                             int omittedDescendantCount) {
        if (omittedFocusNodeCount == 0 && omittedChildCount == 0 && omittedDescendantCount == 0) {
            return null;
        }
        return new Omissions(omittedFocusNodeCount, omittedChildCount, omittedDescendantCount, null,
            Collections.singletonList(OmissionReason.TEXT_BUDGET));
    }

    private Omissions mergeOmissions(Omissions first, Omissions second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        Integer omittedFocusNodeCount = sumCounts(first.getOmittedFocusNodeCount(), second.getOmittedFocusNodeCount());
        Integer omittedChildCount = sumCounts(first.getOmittedChildCount(), second.getOmittedChildCount());
        Integer omittedDescendantCount = sumCounts(first.getOmittedDescendantCount(), second.getOmittedDescendantCount());
        List<OmissionReason> reasons = new ArrayList<>();
        if (first.getOmissionReasons() != null) {
            reasons.addAll(first.getOmissionReasons());
        }
        if (second.getOmissionReasons() != null) {
            for (OmissionReason reason : second.getOmissionReasons()) {
                if (!reasons.contains(reason)) {
                    reasons.add(reason);
                }
            }
        }
        return new Omissions(omittedFocusNodeCount, omittedChildCount, omittedDescendantCount, null, reasons);
    }

    private int measureSerializedLength(Object item) {
        try {
            return objectMapper.writeValueAsBytes(item).length;
        } catch (Exception error) {
            throw new IllegalStateException("Failed to serialize read response.", error);
        }
    }

    private List<String> buildQualifiers(NodeModel nodeModel) {
        if (nodeModel == null) {
            return null;
        }
        List<String> qualifiers = new ArrayList<>();
        if (SummaryNode.isSummaryNode(nodeModel)) {
            qualifiers.add("summary_node");
        }
        if (SummaryNode.isFirstGroupNode(nodeModel)) {
            qualifiers.add("first_group_node");
        }
        if (qualifiers.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableList(qualifiers);
    }

    private UUID parseMapIdentifier(String mapIdentifier) {
        try {
            return UUID.fromString(mapIdentifier);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid map identifier: " + mapIdentifier, error);
        }
    }

    private String requireValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + fieldName);
        }
        return value;
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer sumCounts(Integer first, Integer second) {
        if (first == null && second == null) {
            return null;
        }
        return safeCount(first) + safeCount(second);
    }
}
