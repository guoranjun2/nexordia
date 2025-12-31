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

public class ReadNodeContentToolTest {
    @Test
    public void readNodeContent_returnsFullFocusWithBriefParentAndChildren() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        NodeModel parentNode = mock(NodeModel.class);
        NodeModel firstChildNode = mock(NodeModel.class);
        NodeModel secondChildNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("c33dd4d4-25f0-4bcb-8b57-f6d59cfb57f2");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_focus")).thenReturn(focusNode);
        when(focusNode.getParentNode()).thenReturn(parentNode);
        when(focusNode.getChildren()).thenReturn(Arrays.asList(firstChildNode, secondChildNode));
        NodeContentItem focusItem = new NodeContentItem("ID_focus",
            new NodeContent(null, new TextualContent("Focus", null, null), null, null),
            Collections.emptyList());
        NodeContentItem parentItem = new NodeContentItem("ID_parent",
            new NodeContent("Parent", null, null, null),
            Collections.emptyList());
        NodeContentItem firstChildItem = new NodeContentItem("ID_child_1",
            new NodeContent("Child 1", null, null, null),
            Collections.emptyList());
        NodeContentItem secondChildItem = new NodeContentItem("ID_child_2",
            new NodeContent("Child 2", null, null, null),
            Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(focusNode, NodeContentPreset.FULL)).thenReturn(focusItem);
        when(nodeContentItemReader.readNodeContentItem(parentNode, NodeContentPreset.BRIEF)).thenReturn(parentItem);
        when(nodeContentItemReader.readNodeContentItem(firstChildNode, NodeContentPreset.BRIEF))
            .thenReturn(firstChildItem);
        when(nodeContentItemReader.readNodeContentItem(secondChildNode, NodeContentPreset.BRIEF))
            .thenReturn(secondChildItem);
        AIToolSet uut = new AIToolSet(availableMaps, nodeContentItemReader);

        ReadNodeContentResponse response = uut.readNodeContent(
            new ReadNodeContentRequest(mapIdentifier.toString(), "ID_focus"));

        assertThat(response.getMapIdentifier()).isEqualTo(mapIdentifier.toString());
        assertThat(response.getFocusNode()).isEqualTo(focusItem);
        assertThat(response.getParentNode()).isEqualTo(parentItem);
        assertThat(response.getChildNodes()).containsExactly(firstChildItem, secondChildItem);
        assertThat(response.getFocusNode().getContent().getBriefText()).isNull();
        assertThat(response.getFocusNode().getContent().getTextualContent().getText()).isEqualTo("Focus");
        verify(nodeContentItemReader).readNodeContentItem(focusNode, NodeContentPreset.FULL);
        verify(nodeContentItemReader).readNodeContentItem(parentNode, NodeContentPreset.BRIEF);
        verify(nodeContentItemReader).readNodeContentItem(firstChildNode, NodeContentPreset.BRIEF);
        verify(nodeContentItemReader).readNodeContentItem(secondChildNode, NodeContentPreset.BRIEF);
    }

    @Test
    public void readNodeContent_returnsNullParentWhenFocusIsRoot() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        MapModel mapModel = mock(MapModel.class);
        NodeModel focusNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("1b661e53-7049-4e84-9509-1e7d6e7c9e49");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_root")).thenReturn(focusNode);
        when(focusNode.getParentNode()).thenReturn(null);
        when(focusNode.getChildren()).thenReturn(Collections.emptyList());
        NodeContentItem focusItem = new NodeContentItem("ID_root",
            new NodeContent(null, new TextualContent("Root", null, null), null, null),
            Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(focusNode, NodeContentPreset.FULL)).thenReturn(focusItem);
        AIToolSet uut = new AIToolSet(availableMaps, nodeContentItemReader);

        ReadNodeContentResponse response = uut.readNodeContent(
            new ReadNodeContentRequest(mapIdentifier.toString(), "ID_root"));

        assertThat(response.getFocusNode()).isEqualTo(focusItem);
        assertThat(response.getParentNode()).isNull();
        assertThat(response.getChildNodes()).isEmpty();
        assertThat(response.getFocusNode().getContent().getBriefText()).isNull();
        assertThat(response.getFocusNode().getContent().getTextualContent().getText()).isEqualTo("Root");
    }

    @Test
    public void readNodeContent_throwsWhenMapIdentifierIsInvalid() {
        AIToolSet uut = new AIToolSet(mock(AvailableMaps.class), mock(NodeContentItemReader.class));

        assertThatThrownBy(() -> uut.readNodeContent(
            new ReadNodeContentRequest("not-a-uuid", "ID_focus")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid map identifier");
    }
}
