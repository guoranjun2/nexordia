package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.junit.Test;

public class AttributesContentEditorTest {
    @Test
    public void apply_addsAttributes() {
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModel nodeModel = new NodeModel("node", mapModel);
        AttributesContent attributesContent = new AttributesContent(
            Collections.singletonList(new AttributeEntry("key", "value")));
        AttributesContentEditor unitUnderTest = new AttributesContentEditor();

        unitUnderTest.apply(nodeModel, attributesContent);

        NodeAttributeTableModel attributeTableModel = NodeAttributeTableModel.getModel(nodeModel);
        assertThat(attributeTableModel.getRowCount()).isEqualTo(1);
        assertThat(attributeTableModel.getName(0)).isEqualTo("key");
        assertThat(attributeTableModel.getValue(0)).isEqualTo("value");
    }
}
