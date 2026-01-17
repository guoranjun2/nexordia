package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class SelectionIdentifiersRequest {
    @Description("Selection collection mode. Default: ORDERED.")
    private final SelectionCollectionMode selectionCollectionMode;

    @JsonCreator
    public SelectionIdentifiersRequest(
            @JsonProperty("selectionCollectionMode") SelectionCollectionMode selectionCollectionMode) {
        this.selectionCollectionMode = selectionCollectionMode;
    }

    public SelectionCollectionMode getSelectionCollectionMode() {
        return selectionCollectionMode;
    }
}
