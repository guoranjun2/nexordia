package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeContentEditItem {
    private final EditedElement editedElement;
    @JsonProperty("originalContentType")
    @Description(
        "Original content type read from editableContent. Use MARKDOWN only when the node is already Markdown.")
    private final ContentType originalContentType;
    @JsonProperty(required = false)
    @Description("New content value. For PLAIN_TEXT nodes, formatted text must be wrapped in <html><body>...</body></html>. Use HTML for formatting unless originalContentType is MARKDOWN; Markdown is literal for PLAIN_TEXT.")
    private final String value;
    @JsonProperty(required = false)
    private final Integer index;
    @JsonProperty(required = false)
    @Description("Operation for collection edits (ATTRIBUTES, TAGS, ICONS). Omit for TEXT, DETAILS, or NOTE edits.")
    private final EditOperation operation;
    @JsonProperty(required = false)
    private final String targetKey;

    @JsonCreator
    public NodeContentEditItem(@JsonProperty("editedElement") EditedElement editedElement,
                               @JsonProperty("originalContentType") ContentType originalContentType,
                               @JsonProperty("value") String value,
                               @JsonProperty("index") Integer index,
                               @JsonProperty("operation") EditOperation operation,
                               @JsonProperty("targetKey") String targetKey) {
        this.editedElement = editedElement;
        this.originalContentType = originalContentType;
        this.value = value;
        this.index = index;
        this.operation = operation == null ? EditOperation.REPLACE : operation;
        this.targetKey = targetKey;
    }

    public EditedElement getEditedElement() {
        return editedElement;
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

}
