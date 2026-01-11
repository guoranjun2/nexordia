package org.freeplane.plugin.ai.tools;

import org.freeplane.features.map.NodeModel;

public interface TextContentWriteController {
    void setNodeText(NodeModel nodeModel, String value);

    void setDetails(NodeModel nodeModel, String value);
}
