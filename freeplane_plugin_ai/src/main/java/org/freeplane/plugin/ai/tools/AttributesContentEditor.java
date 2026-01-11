package org.freeplane.plugin.ai.tools;

import java.util.List;

import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.map.NodeModel;

public class AttributesContentEditor {
    public void setInitialContent(NodeModel nodeModel, AttributesContent attributesContent) {
        if (nodeModel == null || attributesContent == null) {
            return;
        }
        List<AttributeEntry> attributes = attributesContent.getAttributes();
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        NodeAttributeTableModel attributeTableModel = NodeAttributeTableModel.getModel(nodeModel);
        for (AttributeEntry attributeEntry : attributes) {
            if (attributeEntry == null || attributeEntry.getName() == null) {
                continue;
            }
            String value = attributeEntry.getValue();
            Attribute attribute = new Attribute(attributeEntry.getName(), value == null ? "" : value);
            attributeTableModel.silentlyAddRowNoUndo(nodeModel, attribute);
        }
    }
}
