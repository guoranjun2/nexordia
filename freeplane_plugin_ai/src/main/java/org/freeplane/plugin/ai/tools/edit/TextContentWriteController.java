package org.freeplane.plugin.ai.tools.edit;

import org.freeplane.features.map.NodeModel;

public interface TextContentWriteController {
    void setNodeText(NodeModel nodeModel, String value);

    void setDetails(NodeModel nodeModel, String value);
}
