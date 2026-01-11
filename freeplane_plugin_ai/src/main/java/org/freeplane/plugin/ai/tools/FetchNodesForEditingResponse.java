package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchNodesForEditingResponse {
    @Description("Map identifier string.")
    private final String mapIdentifier;
    @Description("Node content items with editable content.")
    private final List<NodeContentItem> items;

    @JsonCreator
    public FetchNodesForEditingResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                        @JsonProperty("items") List<NodeContentItem> items) {
        this.mapIdentifier = mapIdentifier;
        this.items = items;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<NodeContentItem> getItems() {
        return items;
    }
}
