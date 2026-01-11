package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.junit.Test;

public class ReadNodeWithContextToolTest {
    @Test
    public void readNodeWithContext_returnsFocusAndSummaryChildrenWithBreadcrumbPath() throws Exception {
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
        when(focusNode.getText()).thenReturn("Focus");
        when(parentNode.getText()).thenReturn("Parent");
        NodeContent focusContent = new NodeContent(null, new TextualContent("Focus full", null, null), null, null, null, null);
        NodeContent firstChildBrief = new NodeContent("Child 1", null, null, null, null, null);
        NodeContent secondChildBrief = new NodeContent("Child 2", null, null, null, null, null);
        NodeContent focusBreadcrumb = new NodeContent("Focus", null, null, null, null, null);
        NodeContent parentBreadcrumb = new NodeContent("Parent", null, null, null, null, null);
        when(nodeContentItemReader.readNodeContent(focusNode, null, NodeContentPreset.FULL)).thenReturn(focusContent);
        when(nodeContentItemReader.readNodeContent(firstChildNode, null, NodeContentPreset.BRIEF)).thenReturn(firstChildBrief);
        when(nodeContentItemReader.readNodeContent(secondChildNode, null, NodeContentPreset.BRIEF)).thenReturn(secondChildBrief);
        when(nodeContentItemReader.readNodeContent(focusNode, null, NodeContentPreset.BRIEF)).thenReturn(focusBreadcrumb);
        when(nodeContentItemReader.readNodeContent(parentNode, null, NodeContentPreset.BRIEF)).thenReturn(parentBreadcrumb);
        TextController textController = mock(TextController.class);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(
            availableMaps, nodeContentItemReader, textController, objectMapper);

        ReadNodesWithContextRequest request = new ReadNodesWithContextRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_focus"),
            Collections.singletonList(ContextSection.BREADCRUMB_PATH),
            null,
            null,
            null,
            null,
            null,
            null);
        ReadNodesWithContextResponse response = uut.readNodeWithContext(request);

        assertThat(response.getMapIdentifier()).isEqualTo(mapIdentifier.toString());
        assertThat(response.getItems()).hasSize(1);
        ReadNodesWithContextItem item = response.getItems().get(0);
        assertThat(item.getBreadcrumbPath()).isEqualTo("Parent/Focus");
        List<NodeDepthItem> nodes = item.getNodes();
        assertThat(nodes).hasSize(3);
        assertThat(nodes.get(0).getNodeIdentifier()).isEqualTo("ID_focus");
        assertThat(nodes.get(0).getDepth()).isEqualTo(0);
        assertThat(nodes.get(0).getContent().getTextualContent().getText()).isEqualTo("Focus full");
        assertThat(nodes.get(1).getNodeIdentifier()).isEqualTo("ID_child_1");
        assertThat(nodes.get(1).getDepth()).isEqualTo(1);
        assertThat(nodes.get(1).getContent().getBriefText()).isEqualTo("Child 1");
        assertThat(nodes.get(2).getNodeIdentifier()).isEqualTo("ID_child_2");
        assertThat(nodes.get(2).getDepth()).isEqualTo(1);
        assertThat(nodes.get(2).getContent().getBriefText()).isEqualTo("Child 2");
        assertThat(nodes.get(0).getQualifiers()).isNull();
    }

    @Test
    public void readNodeWithContext_throwsOnDuplicateNodeIdentifiers() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        UUID mapIdentifier = UUID.fromString("2e8d84f0-75b4-4c76-9c25-46863b02cdde");
        MapModel mapModel = mock(MapModel.class);
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        TextController textController = mock(TextController.class);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(availableMaps, nodeContentItemReader, textController);
        ReadNodesWithContextRequest request = new ReadNodesWithContextRequest(
            mapIdentifier.toString(),
            Arrays.asList("ID_dup", "ID_dup"),
            null,
            0,
            0,
            null,
            null,
            null,
            null);

        assertThatThrownBy(() -> uut.readNodeWithContext(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("duplicate node identifiers");
    }

    @Test
    public void readNodeWithContext_throwsOnUnknownNodeIdentifiers() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        UUID mapIdentifier = UUID.fromString("1b661e53-7049-4e84-9509-1e7d6e7c9e49");
        MapModel mapModel = mock(MapModel.class);
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_missing")).thenReturn(null);
        TextController textController = mock(TextController.class);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(availableMaps, nodeContentItemReader, textController);
        ReadNodesWithContextRequest request = new ReadNodesWithContextRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_missing"),
            null,
            0,
            0,
            null,
            null,
            null,
            null);

        assertThatThrownBy(() -> uut.readNodeWithContext(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown node identifiers: ID_missing");
    }

    @Test
    public void readNodeWithContext_omitsAdditionalFocusNodesWhenBudgetExceeded() throws Exception {
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
        when(nodeContentItemReader.readNodeContent(focusNode, null, NodeContentPreset.FULL))
            .thenReturn(new NodeContent(null, new TextualContent("Focus", null, null), null, null, null, null));
        when(nodeContentItemReader.readNodeContent(secondFocusNode, null, NodeContentPreset.FULL))
            .thenReturn(new NodeContent(null, new TextualContent("Focus 2", null, null), null, null, null, null));
        TextController textController = mock(TextController.class);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(
            availableMaps, nodeContentItemReader, textController, objectMapper);
        ReadNodesWithContextRequest request = new ReadNodesWithContextRequest(
            mapIdentifier.toString(),
            Arrays.asList("ID_focus", "ID_focus_2"),
            null,
            0,
            0,
            150,
            null,
            null,
            null);

        ReadNodesWithContextResponse response = uut.readNodeWithContext(request);

        assertThat(response.getItems()).hasSize(1);
        Omissions omissions = response.getOmissions();
        assertThat(omissions).isNotNull();
        assertThat(omissions.getOmittedFocusNodeCount()).isEqualTo(1);
        assertThat(omissions.getOmissionReasons()).containsExactly(OmissionReason.TEXT_BUDGET);
    }

    @Test
    public void readNodeWithContext_returnsFullContentWithinFullContentDepth() throws Exception {
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
        NodeContent focusFullContent = new NodeContent(null, new TextualContent("Focus full", null, null), null, null, null, null);
        NodeContent childFullContent = new NodeContent(null, new TextualContent("Child full", null, null), null, null, null, null);
        NodeContent focusBriefContent = new NodeContent("Focus", null, null, null, null, null);
        when(nodeContentItemReader.readNodeContent(focusNode, null, NodeContentPreset.FULL)).thenReturn(focusFullContent);
        when(nodeContentItemReader.readNodeContent(childNode, null, NodeContentPreset.FULL)).thenReturn(childFullContent);
        when(nodeContentItemReader.readNodeContent(focusNode, null, NodeContentPreset.BRIEF)).thenReturn(focusBriefContent);
        TextController textController = mock(TextController.class);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(
            availableMaps, nodeContentItemReader, textController, objectMapper);
        ReadNodesWithContextRequest request = new ReadNodesWithContextRequest(
            mapIdentifier.toString(),
            Collections.singletonList("ID_focus"),
            null,
            1,
            0,
            null,
            null,
            null,
            null);

        ReadNodesWithContextResponse response = uut.readNodeWithContext(request);

        ReadNodesWithContextItem item = response.getItems().get(0);
        assertThat(item.getNodes()).hasSize(2);
        assertThat(item.getNodes().get(1).getContent().getTextualContent().getText()).isEqualTo("Child full");
    }
}
