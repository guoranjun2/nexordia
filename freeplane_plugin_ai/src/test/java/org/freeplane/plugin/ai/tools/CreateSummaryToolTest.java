package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.junit.Test;

public class CreateSummaryToolTest {
    @Test
    public void createSummary_returnsSummaryNodeIdentifier() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeModelCreator nodeModelCreator = mock(NodeModelCreator.class);
        NodeInserter nodeInserter = mock(NodeInserter.class);
        SummaryNodeCreator summaryNodeCreator = mock(SummaryNodeCreator.class);
        TextController textController = mock(TextController.class);
        ModifiedNodeSummaryBuilder modifiedNodeSummaryBuilder = new ModifiedNodeSummaryBuilder(textController);
        NodeContentApplier nodeContentApplier = mock(NodeContentApplier.class);
        CreateSummaryTool unitUnderTest = new CreateSummaryTool(availableMaps, nodeModelCreator, nodeInserter,
            summaryNodeCreator,
            modifiedNodeSummaryBuilder, nodeContentApplier);
        UUID mapIdentifier = UUID.fromString("2d872386-bb55-4c6c-9da0-d600b7400b10");
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModel rootNode = new NodeModel("root", mapModel);
        mapModel.setRoot(rootNode);
        NodeModel firstNode = new NodeModel("first", mapModel);
        NodeModel lastNode = new NodeModel("last", mapModel);
        firstNode.setID("ID_first");
        lastNode.setID("ID_last");
        NodeModel summaryNode = new NodeModel("summary", mapModel);
        summaryNode.setID("ID_summary");
        when(availableMaps.findMapModel(mapIdentifier)).thenReturn(mapModel);
        when(summaryNodeCreator.createSummaryNode(rootNode, firstNode, lastNode)).thenReturn(summaryNode);
        NodeCreationItem childItem = new NodeCreationItem(null, Collections.emptyList());
        NodeModel childNodeModel = new NodeModel("child", mapModel);
        childNodeModel.setID("ID_child");
        when(nodeModelCreator.createNodeModelTree(childItem, mapModel)).thenReturn(childNodeModel);
        when(nodeInserter.insertNodes(Collections.singletonList(childNodeModel), summaryNode,
            AnchorPlacementMode.LAST_CHILD)).thenReturn(Collections.singletonList(childNodeModel));
        when(textController.getShortPlainText(summaryNode, 20, " ...")).thenReturn("Summary");
        when(textController.getShortPlainText(childNodeModel, 20, " ...")).thenReturn("Child");
        CreateSummaryRequest request = new CreateSummaryRequest(
            mapIdentifier.toString(),
            "Create summary",
            new SummaryAnchorPlacement("ID_first", "ID_last"),
            Arrays.asList(childItem));

        CreateSummaryResponse response = unitUnderTest.createSummary(request);

        assertThat(response.getSummaryNodeIdentifier()).isEqualTo("ID_summary");
        assertThat(response.getModifiedNodes()).hasSize(2);
        verify(nodeInserter).insertNodes(Collections.singletonList(childNodeModel), summaryNode,
            AnchorPlacementMode.LAST_CHILD);
        verify(nodeContentApplier).apply(childNodeModel, childItem);
    }
}
