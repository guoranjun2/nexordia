package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeContentEditItem {
    private final EditedElement editedElement;
    private final ContentType contentType;
    private final String value;
    private final Integer index;
    private final EditOperation operation;
    private final String targetKey;

    @JsonCreator
    public NodeContentEditItem(@JsonProperty("editedElement") EditedElement editedElement,
                               @JsonProperty("contentType") ContentType contentType,
                               @JsonProperty("value") String value,
                               @JsonProperty("index") Integer index,
                               @JsonProperty("operation") EditOperation operation,
                               @JsonProperty("targetKey") String targetKey) {
        this.editedElement = editedElement;
        this.contentType = contentType;
        this.value = value;
        this.index = index;
        this.operation = operation == null ? EditOperation.REPLACE : operation;
        this.targetKey = targetKey;
    }

    public EditedElement getEditedElement() {
        return editedElement;
    }

    public ContentType getContentType() {
        return contentType;
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
