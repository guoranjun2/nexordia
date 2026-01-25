package org.freeplane.plugin.ai.tools.selection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class SelectionIdentifiersRequest {
    @Description("Selection collection mode (default: ORDERED).")
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
