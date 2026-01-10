package org.freeplane.features.map.mindmapmode;

import java.util.List;

import org.freeplane.features.map.NodeModel;

@FunctionalInterface
public interface OperationErrorHandler {

    /**
     * Handle an operation failure described by the provided text.
     *
     * @param description short description of why the operation failed
     * @param involvedNodes nodes that participated in the failed operation; may be empty but never null
     */
    void handleError(String description, List<NodeModel> involvedNodes);
}
