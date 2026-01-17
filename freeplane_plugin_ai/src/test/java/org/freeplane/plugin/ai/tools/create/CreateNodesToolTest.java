package org.freeplane.plugin.ai.tools.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.OperationErrorHandler;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.freeplane.plugin.ai.tools.content.ModifiedNodeSummaryBuilder;
import org.junit.Test;

public class CreateNodesToolTest {
    @Test
    public void createNodes_returnsModifiedNodeSummariesInOrder() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeCreationHierarchyBuilder nodeCreationHierarchyBuilder = mock(NodeCreationHierarchyBuilder.class);
        NodeInserter nodeInserter = mock(NodeInserter.class);
        TextController textController = mock(TextController.class);
        ModifiedNodeSummaryBuilder modifiedNodeSummaryBuilder = new ModifiedNodeSummaryBuilder(textController);
        CreateNodesTool unitUnderTest = new CreateNodesTool(availableMaps, nodeCreationHierarchyBuilder, nodeInserter,
            modifiedNodeSummaryBuilder);
        UUID mapIdentifier = UUID.fromString("f0ec8744-6a58-4b63-8e0e-9ef00b2e3c7a");
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModel anchorNode = new NodeModel("anchor", mapModel);
        anchorNode.setID("ID_anchor");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        NodeCreationItem firstNodeItem = new NodeCreationItem(0, -1, null);
        NodeCreationItem secondNodeItem = new NodeCreationItem(1, -1, null);
        NodeModel firstNodeModel = new NodeModel("first", mapModel);
        NodeModel secondNodeModel = new NodeModel("second", mapModel);
        firstNodeModel.setID("ID_first");
        secondNodeModel.setID("ID_second");
        when(nodeCreationHierarchyBuilder.buildHierarchy(Arrays.asList(firstNodeItem, secondNodeItem), mapModel))
            .thenReturn(new NodeCreationHierarchy(Arrays.asList(firstNodeModel, secondNodeModel),
                Arrays.asList(firstNodeModel, secondNodeModel)));
        when(nodeInserter.insertNodes(anyList(), any(NodeModel.class), eq(AnchorPlacementMode.LAST_CHILD),
            any(OperationErrorHandler.class)))
            .thenReturn(Arrays.asList(firstNodeModel, secondNodeModel));
        when(textController.getShortPlainText(firstNodeModel, 20, " ...")).thenReturn("First");
        when(textController.getShortPlainText(secondNodeModel, 20, " ...")).thenReturn("Second");
        CreateNodesRequest request = new CreateNodesRequest(
            mapIdentifier.toString(),
            "Create outline",
            new AnchorPlacement("ID_anchor", AnchorPlacementMode.LAST_CHILD),
            Arrays.asList(firstNodeItem, secondNodeItem));

        CreateNodesResponse response = unitUnderTest.createNodes(request);

        assertThat(response.getMapIdentifier()).isEqualTo(mapIdentifier.toString());
        assertThat(response.getUserSummary()).isEqualTo("Create outline");
        assertThat(response.getModifiedNodes()).hasSize(2);
        assertThat(response.getModifiedNodes().get(0).getNodeIdentifier()).isEqualTo("ID_first");
        assertThat(response.getModifiedNodes().get(0).getShortText()).isEqualTo("First");
        assertThat(response.getModifiedNodes().get(1).getNodeIdentifier()).isEqualTo("ID_second");
        assertThat(response.getModifiedNodes().get(1).getShortText()).isEqualTo("Second");
        verify(nodeInserter).insertNodes(anyList(), eq(anchorNode), eq(AnchorPlacementMode.LAST_CHILD),
            any(OperationErrorHandler.class));
        verify(nodeCreationHierarchyBuilder).buildHierarchy(Arrays.asList(firstNodeItem, secondNodeItem), mapModel);
    }
}
