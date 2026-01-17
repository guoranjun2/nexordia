package org.freeplane.plugin.ai.tools.create;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AnchorPlacement {
    private final String anchorNodeIdentifier;
    private final AnchorPlacementMode placementMode;

    @JsonCreator
    public AnchorPlacement(@JsonProperty("anchorNodeIdentifier") String anchorNodeIdentifier,
                           @JsonProperty("placementMode") AnchorPlacementMode placementMode) {
        this.anchorNodeIdentifier = anchorNodeIdentifier;
        this.placementMode = placementMode;
    }

    public String getAnchorNodeIdentifier() {
        return anchorNodeIdentifier;
    }

    public AnchorPlacementMode getPlacementMode() {
        return placementMode;
    }
}
