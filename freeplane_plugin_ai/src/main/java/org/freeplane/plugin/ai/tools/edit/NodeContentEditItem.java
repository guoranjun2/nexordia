package org.freeplane.plugin.ai.tools.edit;

import org.freeplane.plugin.ai.tools.content.ContentType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.langchain4j.model.output.structured.Description;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeContentEditItem {
    @Description("Node ID to edit.")
    private final String nodeIdentifier;
    private final EditedElement editedElement;
    @JsonProperty("originalContentType")
    @Description("Original content type from fetchNodesForEditing.")
    private final ContentType originalContentType;
    @JsonProperty(required = false)
    @Description("New value. For PLAIN_TEXT formatting use HTML; Markdown is literal unless originalContentType is MARKDOWN.")
    private final String value;
    @JsonProperty(required = false)
    private final Integer index;
    @JsonProperty(required = false)
    @Description("Operations: TEXT=REPLACE; DETAILS/NOTE=REPLACE or DELETE; ATTRIBUTES/TAGS/ICONS=ADD/REPLACE/DELETE; "
        + "HYPERLINK=REPLACE/DELETE.")
    private final EditOperation operation;
    @JsonProperty(required = false)
    private final String targetKey;
    @JsonProperty(required = false)
    @Description("Hyperlink value when editedElement is HYPERLINK.")
    private final String hyperlink;

    @JsonCreator
    public NodeContentEditItem(@JsonProperty("nodeIdentifier") String nodeIdentifier,
                               @JsonProperty("editedElement") EditedElement editedElement,
                               @JsonProperty("originalContentType") ContentType originalContentType,
                               @JsonProperty("value") String value,
                               @JsonProperty("index") Integer index,
                               @JsonProperty("operation") EditOperation operation,
                               @JsonProperty("targetKey") String targetKey,
                               @JsonProperty("hyperlink") String hyperlink) {
        this.nodeIdentifier = nodeIdentifier;
        this.editedElement = editedElement;
        this.originalContentType = originalContentType;
        this.value = value;
        this.index = index;
        this.operation = operation == null ? EditOperation.REPLACE : operation;
        this.targetKey = targetKey;
        this.hyperlink = hyperlink;
    }

    public EditedElement getEditedElement() {
        return editedElement;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public ContentType getOriginalContentType() {
        return originalContentType;
    }

    public String getValue() {
        return value;
    }

    public Integer getIndex() {
        return index;
    }

    public EditOperation getOperation() {
        return operation;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public String getHyperlink() {
        return hyperlink;
    }

}
