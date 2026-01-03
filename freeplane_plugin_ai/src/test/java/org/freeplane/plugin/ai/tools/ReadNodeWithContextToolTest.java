package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.junit.Test;

public class ReadNodeWithContextToolTest {
    @Test
    public void readNodeWithContext_returnsDefaultFocusChildrenAndBreadcrumbPath() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        NodeModel parentNode = mock(NodeModel.class);
        NodeModel rootNode = mock(NodeModel.class);
        NodeModel firstChildNode = mock(NodeModel.class);
        NodeModel secondChildNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("c33dd4d4-25f0-4bcb-8b57-f6d59cfb57f2");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.getParentNode()).thenReturn(parentNode);
        when(parentNode.getParentNode()).thenReturn(rootNode);
        when(rootNode.getParentNode()).thenReturn(null);
        when(focusNode.getChildren()).thenReturn(Arrays.asList(firstChildNode, secondChildNode));
        NodeContentItem focusItem = new NodeContentItem("ID_focus",
            new NodeContent(null, new TextualContent("Focus", null, null), null, null),
            Collections.emptyList());
        NodeContentItem firstChildItem = new NodeContentItem("ID_child_1",
            new NodeContent("Child 1", null, null, null),
            Collections.emptyList());
        NodeContentItem secondChildItem = new NodeContentItem("ID_child_2",
            new NodeContent("Child 2", null, null, null),
            Collections.emptyList());
        NodeContentItem focusBreadcrumb = new NodeContentItem(null,
            new NodeContent("Focus", null, null, null),
            Collections.emptyList());
        NodeContentItem parentBreadcrumb = new NodeContentItem(null,
            new NodeContent("Parent", null, null, null),
            Collections.emptyList());
        NodeContentItem rootBreadcrumb = new NodeContentItem(null,
            new NodeContent("Root", null, null, null),
            Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(focusNode, NodeContentPreset.FULL, true))
            .thenReturn(focusItem);
        when(nodeContentItemReader.readNodeContentItem(firstChildNode, NodeContentPreset.BRIEF, true))
            .thenReturn(firstChildItem);
        when(nodeContentItemReader.readNodeContentItem(secondChildNode, NodeContentPreset.BRIEF, true))
            .thenReturn(secondChildItem);
        when(nodeContentItemReader.readNodeContentItem(focusNode, NodeContentPreset.BRIEF, false))
            .thenReturn(focusBreadcrumb);
        when(nodeContentItemReader.readNodeContentItem(parentNode, NodeContentPreset.BRIEF, false))
            .thenReturn(parentBreadcrumb);
        when(nodeContentItemReader.readNodeContentItem(rootNode, NodeContentPreset.BRIEF, false))
            .thenReturn(rootBreadcrumb);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(availableMaps, nodeContentItemReader);

        ReadNodeWithContextResponse response = uut.readNodeWithContext(mapIdentifier.toString(), "ID_focus", null);

        assertThat(response.getMapIdentifier()).isEqualTo(mapIdentifier.toString());
        assertThat(response.getFocusNode()).isEqualTo(focusItem);
        assertThat(response.getParentNode()).isNull();
        assertThat(response.getChildNodes()).containsExactly(firstChildItem, secondChildItem);
        assertThat(response.getBreadcrumbPath()).isEqualTo("Root/Parent/Focus");
        verify(nodeContentItemReader).readNodeContentItem(focusNode, NodeContentPreset.FULL, true);
        verify(nodeContentItemReader).readNodeContentItem(firstChildNode, NodeContentPreset.BRIEF, true);
        verify(nodeContentItemReader).readNodeContentItem(secondChildNode, NodeContentPreset.BRIEF, true);
    }

    @Test
    public void readNodeWithContext_includesParentWhenRequested() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        NodeModel parentNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("1b661e53-7049-4e84-9509-1e7d6e7c9e49");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.getParentNode()).thenReturn(parentNode);
        when(focusNode.getChildren()).thenReturn(Collections.emptyList());
        NodeContentItem focusItem = new NodeContentItem("ID_focus",
            new NodeContent(null, new TextualContent("Focus", null, null), null, null),
            Collections.emptyList());
        NodeContentItem parentItem = new NodeContentItem("ID_parent",
            new NodeContent("Parent", null, null, null),
            Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(focusNode, NodeContentPreset.FULL, true))
            .thenReturn(focusItem);
        when(nodeContentItemReader.readNodeContentItem(parentNode, NodeContentPreset.BRIEF, true))
            .thenReturn(parentItem);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(availableMaps, nodeContentItemReader);

        ReadNodeWithContextResponse response = uut.readNodeWithContext(mapIdentifier.toString(), "ID_focus",
            Arrays.asList(ContextSection.PARENT_SUMMARY, ContextSection.FOCUS_CONTENT));

        assertThat(response.getFocusNode()).isEqualTo(focusItem);
        assertThat(response.getParentNode()).isEqualTo(parentItem);
        assertThat(response.getChildNodes()).isNull();
        assertThat(response.getBreadcrumbPath()).isNull();
    }

    @Test
    public void readNodeWithContext_omitsFocusContentWhenNotRequested() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("2e8d84f0-75b4-4c76-9c25-46863b02cdde");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.getParentNode()).thenReturn(null);
        when(focusNode.getChildren()).thenReturn(Collections.emptyList());
        NodeContentItem focusItem = new NodeContentItem("ID_focus", null, Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(focusNode, (NodeContent) null, true))
            .thenReturn(focusItem);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(availableMaps, nodeContentItemReader);

        ReadNodeWithContextResponse response = uut.readNodeWithContext(mapIdentifier.toString(), "ID_focus",
            Collections.singletonList(ContextSection.PARENT_SUMMARY));

        assertThat(response.getFocusNode()).isEqualTo(focusItem);
        assertThat(response.getFocusNode().getNodeIdentifier()).isEqualTo("ID_focus");
        assertThat(response.getFocusNode().getContent()).isNull();
        assertThat(response.getParentNode()).isNull();
        assertThat(response.getChildNodes()).isNull();
        assertThat(response.getBreadcrumbPath()).isNull();
    }

    @Test
    public void readNodeWithContext_throwsWhenMapIdentifierIsInvalid() {
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(mock(AvailableMaps.class),
            mock(NodeContentItemReader.class));

        assertThatThrownBy(() -> uut.readNodeWithContext("not-a-uuid", "ID_focus", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid map identifier");
    }
}
