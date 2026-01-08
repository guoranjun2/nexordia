package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.junit.Test;

public class MoveNodesIntoSummaryToolTest {
    @Test
    public void moveNodesIntoSummary_createsSummaryNodeAndMovesNodes() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        MMapController mapController = mock(MMapController.class);
        SummaryNodeCreator summaryNodeCreator = mock(SummaryNodeCreator.class);
        TextController textController = mock(TextController.class);
        ModifiedNodeSummaryBuilder modifiedNodeSummaryBuilder = new ModifiedNodeSummaryBuilder(textController);
        MoveNodesIntoSummaryTool unitUnderTest = new MoveNodesIntoSummaryTool(availableMaps, mapController,
            summaryNodeCreator,
            modifiedNodeSummaryBuilder);
        UUID mapIdentifier = UUID.fromString("28f31fd6-7c67-402e-9bb2-9c756498ba7f");
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModel rootNode = new NodeModel("root", mapModel);
        mapModel.setRoot(rootNode);
        NodeModel firstNode = new NodeModel("first", mapModel);
        NodeModel lastNode = new NodeModel("last", mapModel);
        firstNode.setID("ID_first");
        lastNode.setID("ID_last");
        NodeModel firstMovedNode = new NodeModel("moved-1", mapModel);
        NodeModel secondMovedNode = new NodeModel("moved-2", mapModel);
        firstMovedNode.setID("ID_moved_1");
        secondMovedNode.setID("ID_moved_2");
        NodeModel summaryNode = new NodeModel("summary", mapModel);
        summaryNode.setID("ID_summary");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(summaryNodeCreator.createSummaryNode(rootNode, firstNode, lastNode)).thenReturn(summaryNode);
        when(textController.getShortPlainText(summaryNode, 20, " ...")).thenReturn("Summary");
        when(textController.getShortPlainText(firstMovedNode, 20, " ...")).thenReturn("Moved 1");
        when(textController.getShortPlainText(secondMovedNode, 20, " ...")).thenReturn("Moved 2");
        MoveNodesIntoSummaryRequest request = new MoveNodesIntoSummaryRequest(
            mapIdentifier.toString(),
            "Move into summary",
            new SummaryAnchorPlacement("ID_first", "ID_last"),
            Arrays.asList("ID_moved_1", "ID_moved_2"));

        MoveNodesIntoSummaryResponse response = unitUnderTest.moveNodesIntoSummary(request);

        assertThat(response.getSummaryNodeIdentifier()).isEqualTo("ID_summary");
        assertThat(response.getModifiedNodes()).hasSize(3);
        verify(mapController).moveNodes(Arrays.asList(firstMovedNode, secondMovedNode), summaryNode, 0);
    }
}
