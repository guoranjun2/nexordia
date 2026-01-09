package org.freeplane.features.map.mindmapmode;

@FunctionalInterface
public interface OperationErrorHandler {

    /**
     * Handle an operation failure described by the provided text.
     *
     * @param description short description of why the operation failed
     */
    void handleError(String description);
}
