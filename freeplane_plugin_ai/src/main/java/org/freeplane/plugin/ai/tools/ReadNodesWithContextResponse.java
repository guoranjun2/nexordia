package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ReadNodesWithContextResponse {
    private final String mapIdentifier;
    private final List<ReadNodesWithContextItem> items;
    private final Omissions omissions;

    @JsonCreator
    public ReadNodesWithContextResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                        @JsonProperty("items") List<ReadNodesWithContextItem> items,
                                        @JsonProperty("omissions") Omissions omissions) {
        this.mapIdentifier = mapIdentifier;
        this.items = items;
        this.omissions = omissions;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<ReadNodesWithContextItem> getItems() {
        return items;
    }

    public Omissions getOmissions() {
        return omissions;
    }
}
