package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class FetchNodesForEditingRequest {
    @Description("Map identifier string. Use another tool call to refresh identifiers if needed.")
    private final String mapIdentifier;
    @JsonProperty(required = false)
    @Description("List of node identifiers. Default: root node. Use another tool call to refresh identifiers if needed.")
    private final List<String> nodeIdentifiers;
    @JsonProperty(required = false)
    @Description("Editable content fields to include. All representations are returned.")
    private final EditableContentRequest editableContentRequest;

    @JsonCreator
    public FetchNodesForEditingRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                       @JsonProperty("nodeIdentifiers") List<String> nodeIdentifiers,
                                       @JsonProperty("editableContentRequest") EditableContentRequest editableContentRequest) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifiers = nodeIdentifiers;
        this.editableContentRequest = editableContentRequest;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getNodeIdentifiers() {
        return nodeIdentifiers;
    }

    public EditableContentRequest getEditableContentRequest() {
        return editableContentRequest;
    }
}
