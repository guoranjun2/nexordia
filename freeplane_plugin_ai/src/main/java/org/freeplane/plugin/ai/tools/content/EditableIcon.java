package org.freeplane.plugin.ai.tools.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditableIcon {
    private final String description;
    private final Integer index;

    @JsonCreator
    public EditableIcon(@JsonProperty("description") String description,
                        @JsonProperty("index") Integer index) {
        this.description = description;
        this.index = index;
    }

    public String getDescription() {
        return description;
    }

    public Integer getIndex() {
        return index;
    }
}
