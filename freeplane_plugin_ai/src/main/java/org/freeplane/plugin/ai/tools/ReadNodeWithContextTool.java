package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.SummaryNode;
import org.freeplane.plugin.ai.maps.AvailableMaps;

public class ReadNodeWithContextTool {
    private static final EnumSet<ContextSection> DEFAULT_SECTIONS = EnumSet.of(
        ContextSection.BREADCRUMB_PATH,
        ContextSection.FOCUS_CONTENT,
        ContextSection.CHILD_SUMMARIES);

    private final AvailableMaps availableMaps;
    private final NodeContentItemReader nodeContentItemReader;

    public ReadNodeWithContextTool(AvailableMaps availableMaps, NodeContentItemReader nodeContentItemReader) {
        this.availableMaps = Objects.requireNonNull(availableMaps, "availableMaps");
        this.nodeContentItemReader = Objects.requireNonNull(nodeContentItemReader, "nodeContentItemReader");
    }

    public ReadNodeWithContextResponse readNodeWithContext(String mapIdentifier, String nodeIdentifier,
                                                           List<ContextSection> contextSections) {
        String mapIdentifierValue = requireValue(mapIdentifier, "mapIdentifier");
        String nodeIdentifierValue = requireValue(nodeIdentifier, "nodeIdentifier");
        UUID mapIdentifierUuid = parseMapIdentifier(mapIdentifierValue);
        MapModel mapModel = availableMaps.findMapModel(mapIdentifierUuid);
        if (mapModel == null) {
            throw new IllegalArgumentException("Unknown map identifier: " + mapIdentifierValue);
        }
        NodeModel focusNode = mapModel.getNodeForID(nodeIdentifierValue);
        if (focusNode == null) {
            throw new IllegalArgumentException("Unknown node identifier: " + nodeIdentifierValue);
        }
        EnumSet<ContextSection> sections = resolveSections(contextSections);
        NodeContentItem focusNodeItem = readFocusNode(focusNode, sections);
        NodeContentItem parentNodeItem = readParentNode(focusNode, sections);
        List<NodeContentItem> childNodes = readChildNodes(focusNode, sections);
        String breadcrumbPath = sections.contains(ContextSection.BREADCRUMB_PATH)
            ? buildBreadcrumbPath(focusNode)
            : null;
        return new ReadNodeWithContextResponse(mapIdentifierValue, focusNodeItem, parentNodeItem, childNodes,
            breadcrumbPath);
    }

    private EnumSet<ContextSection> resolveSections(List<ContextSection> contextSections) {
        if (contextSections == null || contextSections.isEmpty()) {
            return DEFAULT_SECTIONS;
        }
        EnumSet<ContextSection> sections = EnumSet.noneOf(ContextSection.class);
        for (ContextSection section : contextSections) {
            if (section != null) {
                sections.add(section);
            }
        }
        if (sections.isEmpty()) {
            return DEFAULT_SECTIONS;
        }
        return sections;
    }

    private NodeContentItem readFocusNode(NodeModel focusNode, EnumSet<ContextSection> sections) {
        if (sections.contains(ContextSection.FOCUS_CONTENT)) {
            return nodeContentItemReader.readNodeContentItem(focusNode, NodeContentPreset.FULL, true);
        }
        return nodeContentItemReader.readNodeContentItem(focusNode, (NodeContent) null, true);
    }

    private NodeContentItem readParentNode(NodeModel focusNode, EnumSet<ContextSection> sections) {
        if (!sections.contains(ContextSection.PARENT_SUMMARY)) {
            return null;
        }
        NodeModel parentNode = focusNode.getParentNode();
        if (parentNode == null) {
            return null;
        }
        return nodeContentItemReader.readNodeContentItem(parentNode, NodeContentPreset.BRIEF, true);
    }

    private List<NodeContentItem> readChildNodes(NodeModel focusNode, EnumSet<ContextSection> sections) {
        if (!sections.contains(ContextSection.CHILD_SUMMARIES)) {
            return null;
        }
        List<NodeModel> children = focusNode.getChildren();
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        List<NodeContentItem> childNodes = new ArrayList<>(children.size());
        for (NodeModel childNode : children) {
            childNodes.add(nodeContentItemReader.readNodeContentItem(childNode, NodeContentPreset.BRIEF, true));
        }
        return childNodes;
    }

    private String buildBreadcrumbPath(NodeModel nodeModel) {
        List<String> pathSegments = new ArrayList<>();
        NodeModel current = nodeModel;
        while (current != null) {
            if (!SummaryNode.isHidden(current)) {
                NodeContentItem breadcrumbItem = nodeContentItemReader.readNodeContentItem(
                    current, NodeContentPreset.BRIEF, false);
                String text = null;
                if (breadcrumbItem != null && breadcrumbItem.getContent() != null) {
                    text = breadcrumbItem.getContent().getBriefText();
                }
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
}
