package org.freeplane.plugin.ai.tools.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.freeplane.core.util.Hyperlink;
import org.freeplane.features.link.ConnectorModel;
import org.freeplane.features.link.MapLinks;
import org.freeplane.features.link.NodeLinkModel;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.Clones;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.freeplane.plugin.ai.tools.content.CloneMetadata;
import org.freeplane.plugin.ai.tools.content.ConnectorItem;
import org.freeplane.plugin.ai.tools.content.EditableContent;
import org.freeplane.plugin.ai.tools.content.EditableContentField;
import org.freeplane.plugin.ai.tools.content.NodeContentItem;
import org.freeplane.plugin.ai.tools.content.NodeContentItemReader;
import org.freeplane.plugin.ai.tools.content.NodeContentPreset;
import org.freeplane.plugin.ai.tools.content.NodeContentResponse;
import org.freeplane.plugin.ai.tools.content.TextualContent;
import org.freeplane.plugin.ai.tools.search.OmissionReason;
import org.freeplane.plugin.ai.tools.search.Omissions;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ReadNodesWithDescendantsToolTest {
    private static class TestNodeLinks extends NodeLinks {
        private TestNodeLinks(List<NodeLinkModel> links) {
            super(links);
        }
    }

    @Test
    public void readNodesWithDescendants_returnsFocusAndSummaryChildrenWithBreadcrumbPath() throws Exception {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[100]);
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        NodeModel parentNode = mock(NodeModel.class);
        NodeModel firstChildNode = mock(NodeModel.class);
        NodeModel secondChildNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("c33dd4d4-25f0-4bcb-8b57-f6d59cfb57f2");
        when(availableMaps.findMapModel(eq(mapIdentifier), any())).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.getParentNode()).thenReturn(parentNode);
        when(parentNode.getParentNode()).thenReturn(null);
        when(focusNode.getChildren()).thenReturn(Arrays.asList(firstChildNode, secondChildNode));
        when(focusNode.createID()).thenReturn("ID_focus");
        when(firstChildNode.createID()).thenReturn("ID_child_1");
        when(secondChildNode.createID()).thenReturn("ID_child_2");
        NodeContentResponse focusContent = new NodeContentResponse(
            null, new TextualContent("Focus full", null, null), null, null, null, null, null, null);
        when(nodeContentItemReader.readNodeContent(eq(focusNode), any(), eq(NodeContentPreset.FULL))).thenReturn(focusContent);
        TextController textController = mock(TextController.class);
        when(textController.getShortPlainText(focusNode)).thenReturn("Focus");
        when(textController.getShortPlainText(parentNode)).thenReturn("Parent");
        when(textController.getShortPlainText(firstChildNode)).thenReturn("Child 1");
        when(textController.getShortPlainText(secondChildNode)).thenReturn("Child 2");
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(
            availableMaps, null, nodeContentItemReader, textController, objectMapper);

        ReadNodesWithDescendantsRequest request = new ReadNodesWithDescendantsRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_focus"),
            Collections.singletonList(ContextSection.BREADCRUMB_PATH),
            null,
            null,
            null);
        ReadNodesWithDescendantsResponse response = readTool.readNodesWithDescendants(request);

        assertThat(response.getMapIdentifier()).isEqualTo(mapIdentifier.toString());
        assertThat(response.getItems()).hasSize(1);
        ReadNodesWithDescendantsItem item = response.getItems().get(0);
        assertThat(item.getBreadcrumbPath()).isEqualTo("Parent/Focus");
        List<NodeDepthItem> nodes = item.getNodes();
        assertThat(nodes).hasSize(3);
        assertThat(nodes.get(0).getNodeIdentifier()).isEqualTo("ID_focus");
        assertThat(nodes.get(0).getDepth()).isEqualTo(0);
        assertThat(nodes.get(0).getUnformattedText()).isEqualTo("Text: Focus full");
        assertThat(nodes.get(1).getNodeIdentifier()).isEqualTo("ID_child_1");
        assertThat(nodes.get(1).getDepth()).isEqualTo(1);
        assertThat(nodes.get(1).getUnformattedText()).isEqualTo("Child 1");
        assertThat(nodes.get(2).getNodeIdentifier()).isEqualTo("ID_child_2");
        assertThat(nodes.get(2).getDepth()).isEqualTo(1);
        assertThat(nodes.get(2).getUnformattedText()).isEqualTo("Child 2");
        assertThat(nodes.get(0).getQualifiers()).isNull();
    }

    @Test
    public void readNodesWithDescendants_throwsOnDuplicateNodeIdentifiers() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        UUID mapIdentifier = UUID.fromString("2e8d84f0-75b4-4c76-9c25-46863b02cdde");
        MapModel mapModel = mock(MapModel.class);
        when(availableMaps.findMapModel(eq(mapIdentifier), any())).thenReturn(mapModel);
        TextController textController = mock(TextController.class);
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(availableMaps, null,
            nodeContentItemReader, textController);
        ReadNodesWithDescendantsRequest request = new ReadNodesWithDescendantsRequest(
            mapIdentifier.toString(),
            Arrays.asList("ID_dup", "ID_dup"),
            null,
            0,
            0,
            null);

        assertThatThrownBy(() -> readTool.readNodesWithDescendants(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("duplicate node identifiers");
    }

    @Test
    public void readNodesWithDescendants_throwsOnUnknownNodeIdentifiers() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        UUID mapIdentifier = UUID.fromString("1b661e53-7049-4e84-9509-1e7d6e7c9e49");
        MapModel mapModel = mock(MapModel.class);
        when(availableMaps.findMapModel(eq(mapIdentifier), any())).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_missing")).thenReturn(null);
        TextController textController = mock(TextController.class);
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(availableMaps, null,
            nodeContentItemReader, textController);
        ReadNodesWithDescendantsRequest request = new ReadNodesWithDescendantsRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_missing"),
            null,
            0,
            0,
            null);

        assertThatThrownBy(() -> readTool.readNodesWithDescendants(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown node identifiers: ID_missing");
    }

    @Test
    public void readNodesWithDescendants_omitsAdditionalFocusNodesWhenBudgetExceeded() throws Exception {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[100]);
        UUID mapIdentifier = UUID.fromString("7733f5aa-6722-431e-a0ed-17ef0e67d8e1");
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        NodeModel secondFocusNode = mock(NodeModel.class);
        when(availableMaps.findMapModel(eq(mapIdentifier), any())).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(mapModel.getNodeForID("ID_focus_2")).thenReturn(secondFocusNode);
        when(focusNode.createID()).thenReturn("ID_focus");
        when(secondFocusNode.createID()).thenReturn("ID_focus_2");
        when(focusNode.getChildren()).thenReturn(Collections.emptyList());
        when(secondFocusNode.getChildren()).thenReturn(Collections.emptyList());
        when(nodeContentItemReader.readNodeContent(eq(focusNode), any(), eq(NodeContentPreset.FULL)))
            .thenReturn(new NodeContentResponse(
                null, new TextualContent("Focus", null, null), null, null, null, null, null, null));
        when(nodeContentItemReader.readNodeContent(eq(secondFocusNode), any(), eq(NodeContentPreset.FULL)))
            .thenReturn(new NodeContentResponse(
                null, new TextualContent("Focus 2", null, null), null, null, null, null, null, null));
        TextController textController = mock(TextController.class);
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(
            availableMaps, null, nodeContentItemReader, textController, objectMapper);
        ReadNodesWithDescendantsRequest request = new ReadNodesWithDescendantsRequest(
            mapIdentifier.toString(),
            Arrays.asList("ID_focus", "ID_focus_2"),
            null,
            0,
            0,
            150);

        ReadNodesWithDescendantsResponse response = readTool.readNodesWithDescendants(request);

        assertThat(response.getItems()).hasSize(1);
        Omissions omissions = response.getOmissions();
        assertThat(omissions).isNotNull();
        assertThat(omissions.getOmittedFocusNodeCount()).isEqualTo(1);
        assertThat(omissions.getOmissionReasons()).containsExactly(OmissionReason.TEXT_BUDGET);
    }

    @Test
    public void readNodesWithDescendants_returnsFullContentWithinFullContentDepth() throws Exception {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[100]);
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        NodeModel childNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("bd2f43b2-f1b4-4a3a-b41b-259d5c3427bf");
        when(availableMaps.findMapModel(eq(mapIdentifier), any())).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.getParentNode()).thenReturn(null);
        when(focusNode.getChildren()).thenReturn(Collections.singletonList(childNode));
        when(childNode.getChildren()).thenReturn(Collections.emptyList());
        when(focusNode.createID()).thenReturn("ID_focus");
        when(childNode.createID()).thenReturn("ID_child");
        NodeContentResponse focusFullContent = new NodeContentResponse(
            null, new TextualContent("Focus full", null, null), null, null, null, null, null, null);
        NodeContentResponse childFullContent = new NodeContentResponse(
            null, new TextualContent("Child full", null, null), null, null, null, null, null, null);
        when(nodeContentItemReader.readNodeContent(eq(focusNode), any(), eq(NodeContentPreset.FULL))).thenReturn(focusFullContent);
        when(nodeContentItemReader.readNodeContent(eq(childNode), any(), eq(NodeContentPreset.FULL))).thenReturn(childFullContent);
        TextController textController = mock(TextController.class);
        when(textController.getShortPlainText(childNode)).thenReturn("Child");
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(
            availableMaps, null, nodeContentItemReader, textController, objectMapper);
        ReadNodesWithDescendantsRequest request = new ReadNodesWithDescendantsRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_focus"),
            null,
            1,
            0,
            null);

        ReadNodesWithDescendantsResponse response = readTool.readNodesWithDescendants(request);

        ReadNodesWithDescendantsItem item = response.getItems().get(0);
        assertThat(item.getNodes()).hasSize(2);
        assertThat(item.getNodes().get(1).getUnformattedText()).isEqualTo("Text: Child full");
    }

    @Test
    public void readNodesWithDescendants_includesLinkAndCloneMetadataWhenRequested() throws Exception {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[100]);
        UUID mapIdentifier = UUID.fromString("41c2e9d7-ff86-4d64-b81c-7a6cddc69fe7");
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        NodeModel targetNode = mock(NodeModel.class);
        NodeModel incomingSource = mock(NodeModel.class);
        NodeModel cloneNode = mock(NodeModel.class);
        when(availableMaps.findMapModel(eq(mapIdentifier), any())).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.getChildren()).thenReturn(Collections.emptyList());
        when(focusNode.createID()).thenReturn("ID_focus");
        when(focusNode.getID()).thenReturn("ID_focus");
        when(focusNode.hasID()).thenReturn(true);
        when(focusNode.getMap()).thenReturn(mapModel);
        when(targetNode.createID()).thenReturn("ID_target");
        when(incomingSource.createID()).thenReturn("ID_source");
        when(cloneNode.createID()).thenReturn("ID_clone");
        when(focusNode.isCloneTreeRoot()).thenReturn(true);
        when(focusNode.isCloneTreeNode()).thenReturn(false);
        when(focusNode.allClones()).thenReturn(new TestClones(Arrays.asList(focusNode, cloneNode)));
        NodeContentResponse focusContent = new NodeContentResponse(null,
            new TextualContent("Focus full", null, null), null, null, null, null, null, null);
        when(nodeContentItemReader.readNodeContent(eq(focusNode), any(), eq(NodeContentPreset.FULL)))
            .thenReturn(focusContent);
        TextController textController = mock(TextController.class);

        ConnectorModel outgoingConnector = mock(ConnectorModel.class);
        when(outgoingConnector.getSource()).thenReturn(focusNode);
        when(outgoingConnector.getTargetID()).thenReturn("ID_target");
        when(outgoingConnector.getSourceLabel()).thenReturn(Optional.of("out-source"));
        when(outgoingConnector.getMiddleLabel()).thenReturn(Optional.of("out-middle"));
        when(outgoingConnector.getTargetLabel()).thenReturn(Optional.of("out-target"));
        when(outgoingConnector.cloneForSource(focusNode)).thenReturn(outgoingConnector);
        List<NodeLinkModel> outgoingLinks = Arrays.asList(outgoingConnector);
        NodeLinks nodeLinks = new TestNodeLinks(outgoingLinks);
        nodeLinks.setHyperLink(new Hyperlink(URI.create("https://example.com")));
        when(focusNode.getExtension(NodeLinks.class)).thenReturn(nodeLinks);

        ConnectorModel incomingConnector = mock(ConnectorModel.class);
        when(incomingConnector.getSource()).thenReturn(incomingSource);
        when(incomingConnector.getTargetID()).thenReturn("ID_focus");
        when(incomingConnector.getSourceLabel()).thenReturn(Optional.of("in-source"));
        when(incomingConnector.getMiddleLabel()).thenReturn(Optional.of("in-middle"));
        when(incomingConnector.getTargetLabel()).thenReturn(Optional.of("in-target"));
        MapLinks mapLinks = new MapLinks();
        mapLinks.add(incomingConnector);
        when(mapModel.getExtension(MapLinks.class)).thenReturn(mapLinks);

        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(
            availableMaps, null, nodeContentItemReader, textController, objectMapper);
        ReadNodesWithDescendantsRequest request = new ReadNodesWithDescendantsRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_focus"),
            Arrays.asList(ContextSection.HYPERLINK, ContextSection.OUTGOING_CONNECTORS,
                ContextSection.INCOMING_CONNECTORS, ContextSection.CLONE_METADATA),
            0,
            0,
            null);

        ReadNodesWithDescendantsResponse response = readTool.readNodesWithDescendants(request);

        NodeDepthItem node = response.getItems().get(0).getNodes().get(0);
        assertThat(node.getHyperlink()).isEqualTo("https://example.com");
        assertThat(node.getOutgoingConnectors()).hasSize(1);
        ConnectorItem outgoingItem = node.getOutgoingConnectors().get(0);
        assertThat(outgoingItem.getSourceNodeIdentifier()).isEqualTo("ID_focus");
        assertThat(outgoingItem.getTargetNodeIdentifier()).isEqualTo("ID_target");
        assertThat(outgoingItem.getSourceLabel()).isEqualTo("out-source");
        assertThat(outgoingItem.getMiddleLabel()).isEqualTo("out-middle");
        assertThat(outgoingItem.getTargetLabel()).isEqualTo("out-target");
        assertThat(node.getIncomingConnectors()).hasSize(1);
        ConnectorItem incomingItem = node.getIncomingConnectors().get(0);
        assertThat(incomingItem.getSourceNodeIdentifier()).isEqualTo("ID_source");
        assertThat(incomingItem.getTargetNodeIdentifier()).isEqualTo("ID_focus");
        assertThat(incomingItem.getSourceLabel()).isEqualTo("in-source");
        assertThat(incomingItem.getMiddleLabel()).isEqualTo("in-middle");
        assertThat(incomingItem.getTargetLabel()).isEqualTo("in-target");
        CloneMetadata cloneMetadata = node.getCloneMetadata();
        assertThat(cloneMetadata).isNotNull();
        assertThat(cloneMetadata.getCloneNodeIdentifiers()).containsExactly("ID_clone");
        assertThat(cloneMetadata.isCloneTreeRoot()).isTrue();
        assertThat(cloneMetadata.isCloneTreeNode()).isFalse();
    }

    @Test
    public void fetchNodesForEditing_returnsEditableContentOnly() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        UUID mapIdentifier = UUID.fromString("bb7f2976-43e0-4bf7-9cc1-77a0949f4f30");
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        when(availableMaps.findMapModel(eq(mapIdentifier), any())).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.createID()).thenReturn("ID_focus");
        EditableContent editableContent = mock(EditableContent.class);
        NodeContentResponse content = new NodeContentResponse(null, null, null, null, null, null, null, editableContent);
        when(nodeContentItemReader.readNodeContent(eq(focusNode), any(), eq(NodeContentPreset.FULL)))
            .thenReturn(content);
        NodeContentItem item = new NodeContentItem("ID_focus", content, null, null, null, null, null);
        when(nodeContentItemReader.readNodeContentItem(focusNode, content, true, false)).thenReturn(item);
        TextController textController = mock(TextController.class);
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(availableMaps, null,
            nodeContentItemReader, textController);
        FetchNodesForEditingRequest request = new FetchNodesForEditingRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_focus"),
            Collections.singletonList(EditableContentField.TEXT));

        FetchNodesForEditingResponse response = readTool.fetchNodesForEditing(request);

        assertThat(response.getMapIdentifier()).isEqualTo(mapIdentifier.toString());
        assertThat(response.getItems()).containsExactly(item);
    }

    @Test
    public void fetchNodesForEditing_throwsOnMissingEditableContentFields() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        TextController textController = mock(TextController.class);
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(availableMaps, null,
            nodeContentItemReader, textController);
        UUID mapIdentifier = UUID.fromString("58d1888f-151a-4b12-ac54-7f6ff94c876f");
        FetchNodesForEditingRequest request = new FetchNodesForEditingRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_focus"),
            null);

        assertThatThrownBy(() -> readTool.fetchNodesForEditing(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Missing editableContentFields");
    }

    @Test
    public void fetchNodesForEditing_throwsOnEmptyEditableContentFields() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        TextController textController = mock(TextController.class);
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(availableMaps, null,
            nodeContentItemReader, textController);
        UUID mapIdentifier = UUID.fromString("ebf35466-149f-472f-95f8-13b07bd634cb");
        FetchNodesForEditingRequest request = new FetchNodesForEditingRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_focus"),
            Collections.<EditableContentField>emptyList());

        assertThatThrownBy(() -> readTool.fetchNodesForEditing(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Missing editableContentFields");
    }

    private static class TestClones implements Clones {
        private final List<NodeModel> nodes;

        private TestClones(List<NodeModel> nodes) {
            this.nodes = nodes;
        }

        @Override
        public int size() {
            return nodes.size();
        }

        @Override
        public void attach() {
        }

        @Override
        public void detach(NodeModel nodeModel) {
        }

        @Override
        public Clones add(NodeModel clone) {
            return this;
        }

        @Override
        public Collection<NodeModel> toCollection() {
            return nodes;
        }

        @Override
        public boolean contains(NodeModel node) {
            return nodes.contains(node);
        }

        @Override
        public NodeModel head() {
            return nodes.isEmpty() ? null : nodes.get(0);
        }

        @Override
        public NodeModel.CloneType getCloneType() {
            return NodeModel.CloneType.CONTENT;
        }

        @Override
        public Iterator<NodeModel> iterator() {
            return nodes.iterator();
        }
    }
}
