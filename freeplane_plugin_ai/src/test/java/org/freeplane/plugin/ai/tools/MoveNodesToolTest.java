package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

public class MoveNodesToolTest {
    @Test
    public void moveNodes_movesNodesWithAnchorPlacement() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        MMapController mapController = mock(MMapController.class);
        AnchorPlacementCalculator anchorPlacementCalculator = new AnchorPlacementCalculator();
        TextController textController = mock(TextController.class);
        ModifiedNodeSummaryBuilder modifiedNodeSummaryBuilder = new ModifiedNodeSummaryBuilder(textController);
        MoveNodesTool unitUnderTest = new MoveNodesTool(availableMaps, mapController, anchorPlacementCalculator,
            modifiedNodeSummaryBuilder);
        UUID mapIdentifier = UUID.fromString("c2d087aa-9470-4ab9-8dfd-45f2d9670f44");
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModel parentNode = new NodeModel("parent", mapModel);
        NodeModel anchorNode = new NodeModel("anchor", mapModel);
        parentNode.insert(anchorNode, 0);
        anchorNode.setID("ID_anchor");
        NodeModel firstNode = new NodeModel("first", mapModel);
        NodeModel secondNode = new NodeModel("second", mapModel);
        firstNode.setID("ID_first");
        secondNode.setID("ID_second");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(textController.getShortPlainText(firstNode, 20, " ...")).thenReturn("First");
        when(textController.getShortPlainText(secondNode, 20, " ...")).thenReturn("Second");
        when(mapController.isWriteable(any(NodeModel.class))).thenReturn(true);
        MoveNodesRequest request = new MoveNodesRequest(
            mapIdentifier.toString(),
            "Reorder nodes",
            new AnchorPlacement("ID_anchor", AnchorPlacementMode.SIBLING_AFTER),
            Arrays.asList("ID_first", "ID_second"));

        MoveNodesResponse response = unitUnderTest.moveNodes(request);

        assertThat(response.getMapIdentifier()).isEqualTo(mapIdentifier.toString());
        assertThat(response.getUserSummary()).isEqualTo("Reorder nodes");
        assertThat(response.getModifiedNodes()).hasSize(2);
        verify(mapController).moveNodes(Arrays.asList(firstNode, secondNode), parentNode, 1);
    }
}
