package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.freeplane.features.map.FirstGroupNodeFlag;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.SummaryNodeFlag;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.junit.Test;

public class BreadcrumbsToolTest {
    @Test
    public void getBreadcrumbs_returnsPathWithIdentifiers() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        MapModel mapModel = mock(MapModel.class);
        NodeModel rootNode = mock(NodeModel.class);
        NodeModel parentNode = mock(NodeModel.class);
        NodeModel targetNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("d7d1a45d-62f7-4ec6-9b4a-7f41f9f8b8a3");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_target")).thenReturn(targetNode);
        when(targetNode.getParentNode()).thenReturn(parentNode);
        when(parentNode.getParentNode()).thenReturn(rootNode);
        when(rootNode.getParentNode()).thenReturn(null);
        NodeContentItem rootItem = new NodeContentItem("ID_root",
            new NodeContentResponse("Root", null, null, null, null, null), Collections.emptyList());
        NodeContentItem parentItem = new NodeContentItem("ID_parent",
            new NodeContentResponse("Parent", null, null, null, null, null), Collections.emptyList());
        NodeContentItem targetItem = new NodeContentItem("ID_target",
            new NodeContentResponse("Target", null, null, null, null, null), Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(rootNode, NodeContentPreset.BRIEF, true)).thenReturn(rootItem);
        when(nodeContentItemReader.readNodeContentItem(parentNode, NodeContentPreset.BRIEF, true)).thenReturn(parentItem);
        when(nodeContentItemReader.readNodeContentItem(targetNode, NodeContentPreset.BRIEF, true)).thenReturn(targetItem);
        BreadcrumbsTool uut = new BreadcrumbsTool(availableMaps, nodeContentItemReader);

        BreadcrumbsResponse response = uut.getBreadcrumbs(
            new BreadcrumbsRequest(mapIdentifier.toString(), "ID_target", true));

        assertThat(response.getBreadcrumbs())
            .extracting(BreadcrumbItem::getText, BreadcrumbItem::getNodeIdentifier)
            .containsExactly(
                tuple("Root", "ID_root"),
                tuple("Parent", "ID_parent"),
                tuple("Target", "ID_target"));
    }

    @Test
    public void getBreadcrumbs_skipsHiddenSummaryNodes() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        MapModel mapModel = mock(MapModel.class);
        NodeModel rootNode = mock(NodeModel.class);
        NodeModel hiddenNode = mock(NodeModel.class);
        NodeModel targetNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("4c0c8af1-8a97-4aad-bc46-83d0c10c9e93");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_target")).thenReturn(targetNode);
        when(targetNode.getParentNode()).thenReturn(hiddenNode);
        when(hiddenNode.getParentNode()).thenReturn(rootNode);
        when(rootNode.getParentNode()).thenReturn(null);
        when(hiddenNode.isFolded()).thenReturn(false);
        when(hiddenNode.hasChildren()).thenReturn(true);
        when(hiddenNode.containsExtension(SummaryNodeFlag.class)).thenReturn(true);
        when(hiddenNode.containsExtension(FirstGroupNodeFlag.class)).thenReturn(false);
        when(hiddenNode.getText()).thenReturn("");
        NodeContentItem rootItem = new NodeContentItem("ID_root",
            new NodeContentResponse("Root", null, null, null, null, null), Collections.emptyList());
        NodeContentItem targetItem = new NodeContentItem("ID_target",
            new NodeContentResponse("Target", null, null, null, null, null), Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(rootNode, NodeContentPreset.BRIEF, true)).thenReturn(rootItem);
        when(nodeContentItemReader.readNodeContentItem(targetNode, NodeContentPreset.BRIEF, true)).thenReturn(targetItem);
        BreadcrumbsTool uut = new BreadcrumbsTool(availableMaps, nodeContentItemReader);

        BreadcrumbsResponse response = uut.getBreadcrumbs(
            new BreadcrumbsRequest(mapIdentifier.toString(), "ID_target", true));

        assertThat(response.getBreadcrumbs())
            .extracting(BreadcrumbItem::getText, BreadcrumbItem::getNodeIdentifier)
            .containsExactly(
                tuple("Root", "ID_root"),
                tuple("Target", "ID_target"));
        verify(nodeContentItemReader, never())
            .readNodeContentItem(hiddenNode, NodeContentPreset.BRIEF, true);
    }

    @Test
    public void getBreadcrumbs_omitsNodeIdentifiersWhenDisabled() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        MapModel mapModel = mock(MapModel.class);
        NodeModel targetNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("8f1bf5ea-8d9e-4b91-a9b2-fc1ea5701e98");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_target")).thenReturn(targetNode);
        when(targetNode.getParentNode()).thenReturn(null);
        NodeContentItem targetItem = new NodeContentItem("ID_target",
            new NodeContentResponse("Target", null, null, null, null, null), Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(targetNode, NodeContentPreset.BRIEF, false))
            .thenReturn(targetItem);
        BreadcrumbsTool uut = new BreadcrumbsTool(availableMaps, nodeContentItemReader);

        BreadcrumbsResponse response = uut.getBreadcrumbs(
            new BreadcrumbsRequest(mapIdentifier.toString(), "ID_target", false));

        assertThat(response.getBreadcrumbs())
            .extracting(BreadcrumbItem::getText, BreadcrumbItem::getNodeIdentifier)
            .containsExactly(tuple("Target", null));
        verify(nodeContentItemReader).readNodeContentItem(targetNode, NodeContentPreset.BRIEF, false);
    }

    @Test
    public void getBreadcrumbs_throwsWhenMapIdentifierIsInvalid() {
        BreadcrumbsTool uut = new BreadcrumbsTool(mock(AvailableMaps.class), mock(NodeContentItemReader.class));

        assertThatThrownBy(() -> uut.getBreadcrumbs(
            new BreadcrumbsRequest("not-a-uuid", "ID_target", true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid map identifier");
    }

    @Test
    public void getBreadcrumbs_throwsWhenNodeIdentifierIsUnknown() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        MapModel mapModel = mock(MapModel.class);
        UUID mapIdentifier = UUID.fromString("cb18bb44-c208-4bb6-9d03-70ef5287e7dc");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(mapModel.getNodeForID("ID_target")).thenReturn(null);
        BreadcrumbsTool uut = new BreadcrumbsTool(availableMaps, mock(NodeContentItemReader.class));

        assertThatThrownBy(() -> uut.getBreadcrumbs(
            new BreadcrumbsRequest(mapIdentifier.toString(), "ID_target", true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown node identifier");
    }
}
