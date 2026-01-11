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

    @JsonCreator
    public NodeContentEditItem(@JsonProperty("editedElement") EditedElement editedElement,
                               @JsonProperty("contentType") ContentType contentType,
                               @JsonProperty("value") String value) {
                               @JsonProperty("index") Integer index) {
        this.editedElement = editedElement;
        this.contentType = contentType;
        this.value = value;
        this.index = index;
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
}
