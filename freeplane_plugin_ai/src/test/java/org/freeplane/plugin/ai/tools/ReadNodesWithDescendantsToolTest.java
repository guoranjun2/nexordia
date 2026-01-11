package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ReadNodesWithDescendantsToolTest {
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
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.getParentNode()).thenReturn(parentNode);
        when(parentNode.getParentNode()).thenReturn(null);
        when(focusNode.getChildren()).thenReturn(Arrays.asList(firstChildNode, secondChildNode));
        when(focusNode.createID()).thenReturn("ID_focus");
        when(firstChildNode.createID()).thenReturn("ID_child_1");
        when(secondChildNode.createID()).thenReturn("ID_child_2");
        NodeContentResponse focusContent = new NodeContentResponse(null, new TextualContent("Focus full", null, null), null, null, null, null);
        when(nodeContentItemReader.readNodeContent(eq(focusNode), any(), eq(NodeContentPreset.FULL))).thenReturn(focusContent);
        TextController textController = mock(TextController.class);
        when(textController.getShortPlainText(focusNode)).thenReturn("Focus");
        when(textController.getShortPlainText(parentNode)).thenReturn("Parent");
        when(textController.getShortPlainText(firstChildNode)).thenReturn("Child 1");
        when(textController.getShortPlainText(secondChildNode)).thenReturn("Child 2");
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(
            availableMaps, nodeContentItemReader, textController, objectMapper);

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
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        TextController textController = mock(TextController.class);
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(availableMaps, nodeContentItemReader, textController);
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
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_missing")).thenReturn(null);
        TextController textController = mock(TextController.class);
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(availableMaps, nodeContentItemReader, textController);
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
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(mapModel.getNodeForID("ID_focus_2")).thenReturn(secondFocusNode);
        when(focusNode.createID()).thenReturn("ID_focus");
        when(secondFocusNode.createID()).thenReturn("ID_focus_2");
        when(focusNode.getChildren()).thenReturn(Collections.emptyList());
        when(secondFocusNode.getChildren()).thenReturn(Collections.emptyList());
        when(nodeContentItemReader.readNodeContent(eq(focusNode), any(), eq(NodeContentPreset.FULL)))
            .thenReturn(new NodeContentResponse(null, new TextualContent("Focus", null, null), null, null, null, null));
        when(nodeContentItemReader.readNodeContent(eq(secondFocusNode), any(), eq(NodeContentPreset.FULL)))
            .thenReturn(new NodeContentResponse(null, new TextualContent("Focus 2", null, null), null, null, null, null));
        TextController textController = mock(TextController.class);
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(
            availableMaps, nodeContentItemReader, textController, objectMapper);
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
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.getParentNode()).thenReturn(null);
        when(focusNode.getChildren()).thenReturn(Collections.singletonList(childNode));
        when(childNode.getChildren()).thenReturn(Collections.emptyList());
        when(focusNode.createID()).thenReturn("ID_focus");
        when(childNode.createID()).thenReturn("ID_child");
        NodeContentResponse focusFullContent = new NodeContentResponse(null, new TextualContent("Focus full", null, null), null, null, null, null);
        NodeContentResponse childFullContent = new NodeContentResponse(null, new TextualContent("Child full", null, null), null, null, null, null);
        when(nodeContentItemReader.readNodeContent(eq(focusNode), any(), eq(NodeContentPreset.FULL))).thenReturn(focusFullContent);
        when(nodeContentItemReader.readNodeContent(eq(childNode), any(), eq(NodeContentPreset.FULL))).thenReturn(childFullContent);
        TextController textController = mock(TextController.class);
        when(textController.getShortPlainText(childNode)).thenReturn("Child");
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(
            availableMaps, nodeContentItemReader, textController, objectMapper);
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
    public void fetchNodesForEditing_returnsEditableContentOnly() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        UUID mapIdentifier = UUID.fromString("bb7f2976-43e0-4bf7-9cc1-77a0949f4f30");
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.createID()).thenReturn("ID_focus");
        EditableContent editableContent = mock(EditableContent.class);
        NodeContentResponse content = new NodeContentResponse(null, null, null, null, null, editableContent);
        when(nodeContentItemReader.readNodeContent(eq(focusNode), any(), eq(NodeContentPreset.FULL)))
            .thenReturn(content);
        NodeContentItem item = new NodeContentItem("ID_focus", content, null);
        when(nodeContentItemReader.readNodeContentItem(focusNode, content, true, false)).thenReturn(item);
        TextController textController = mock(TextController.class);
        ReadNodesWithDescendantsTool readTool = new ReadNodesWithDescendantsTool(availableMaps, nodeContentItemReader, textController);
        FetchNodesForEditingRequest request = new FetchNodesForEditingRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_focus"),
            new EditableContentRequest(Collections.singletonList(EditableContentField.TEXT)));

        FetchNodesForEditingResponse response = readTool.fetchNodesForEditing(request);

        assertThat(response.getMapIdentifier()).isEqualTo(mapIdentifier.toString());
        assertThat(response.getItems()).containsExactly(item);
    }
}
