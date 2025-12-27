package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class MoveNodesResponse {
    private final String mapIdentifier;
    private final List<String> movedNodeIdentifiers;

    public MoveNodesResponse(String mapIdentifier, List<String> movedNodeIdentifiers) {
        this.mapIdentifier = mapIdentifier;
        this.movedNodeIdentifiers = movedNodeIdentifiers;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getMovedNodeIdentifiers() {
        return movedNodeIdentifiers;
    }
}
