package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.junit.Test;

public class NodeCreationHierarchyBuilderTest {
    @Test
    public void buildHierarchy_preservesSiblingOrderFromInputList() {
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModelCreator nodeModelCreator = new NodeModelCreator();
        NodeContentApplier nodeContentApplier = mock(NodeContentApplier.class);
        doAnswer(invocation -> {
            NodeModel nodeModel = invocation.getArgument(0);
            NodeContentWriteRequest content = invocation.getArgument(1);
            if (content != null && content.getText() != null) {
                nodeModel.setText(content.getText());
            }
            return null;
        }).when(nodeContentApplier).apply(any(NodeModel.class), any(NodeContentWriteRequest.class));
        NodeCreationHierarchyBuilder builder = new NodeCreationHierarchyBuilder(nodeModelCreator, nodeContentApplier);

        NodeCreationItem root = new NodeCreationItem(0, -1, new NodeContentWriteRequest(
            "root", null, null, null, null, null, null, null, null));
        NodeCreationItem childTwo = new NodeCreationItem(2, 0, new NodeContentWriteRequest(
            "childTwo", null, null, null, null, null, null, null, null));
        NodeCreationItem childOne = new NodeCreationItem(1, 0, new NodeContentWriteRequest(
            "childOne", null, null, null, null, null, null, null, null));

        NodeCreationHierarchy hierarchy = builder.buildHierarchy(Arrays.asList(root, childTwo, childOne), mapModel);

        assertThat(hierarchy.getRootNodes()).hasSize(1);
        NodeModel rootNode = hierarchy.getRootNodes().get(0);
        assertThat(rootNode.getChildCount()).isEqualTo(2);
        assertThat(rootNode.getChildAt(0).getText()).isEqualTo("childTwo");
        assertThat(rootNode.getChildAt(1).getText()).isEqualTo("childOne");
    }

    @Test
    public void buildHierarchy_rejectsUnknownParentIndex() {
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModelCreator nodeModelCreator = new NodeModelCreator();
        NodeCreationHierarchyBuilder builder = new NodeCreationHierarchyBuilder(nodeModelCreator, mock(NodeContentApplier.class));
        NodeCreationItem root = new NodeCreationItem(0, -1, null);
        NodeCreationItem orphan = new NodeCreationItem(1, 99, null);

        assertThatThrownBy(() -> builder.buildHierarchy(Arrays.asList(root, orphan), mapModel))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown parentIndex: 99");
    }
}
