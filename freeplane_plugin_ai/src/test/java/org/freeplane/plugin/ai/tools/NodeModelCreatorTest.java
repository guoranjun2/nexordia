package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.junit.Test;

public class NodeModelCreatorTest {
    @Test
    public void createNodeModelTree_preservesChildOrder() {
        NodeCreationItem firstChild = new NodeCreationItem(null, Collections.emptyList());
        NodeCreationItem secondChild = new NodeCreationItem(null, Collections.emptyList());
        NodeCreationItem rootItem = new NodeCreationItem(null, Arrays.asList(firstChild, secondChild));
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModelCreator unitUnderTest = new NodeModelCreator();

        NodeModel rootNode = unitUnderTest.createNodeModelTree(rootItem, mapModel);

        assertThat(rootNode.getChildCount()).isEqualTo(2);
        assertThat(rootNode.getChildAt(0)).isNotNull();
        assertThat(rootNode.getChildAt(1)).isNotNull();
    }

    @Test
    public void createNodeModelTree_buildsNestedChildrenInOrder() {
        NodeCreationItem nestedFirst = new NodeCreationItem(null, Collections.emptyList());
        NodeCreationItem nestedSecond = new NodeCreationItem(null, Collections.emptyList());
        NodeCreationItem childWithNested = new NodeCreationItem(null, Arrays.asList(nestedFirst, nestedSecond));
        NodeCreationItem otherChild = new NodeCreationItem(null, Collections.emptyList());
        NodeCreationItem rootItem = new NodeCreationItem(null, Arrays.asList(childWithNested, otherChild));
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModelCreator unitUnderTest = new NodeModelCreator();

        NodeModel rootNode = unitUnderTest.createNodeModelTree(rootItem, mapModel);

        NodeModel firstChildNode = rootNode.getChildAt(0);
        assertThat(firstChildNode.getChildCount()).isEqualTo(2);
        assertThat(firstChildNode.getChildAt(0)).isNotNull();
        assertThat(firstChildNode.getChildAt(1)).isNotNull();
        assertThat(rootNode.getChildAt(1).getChildCount()).isEqualTo(0);
    }
}
