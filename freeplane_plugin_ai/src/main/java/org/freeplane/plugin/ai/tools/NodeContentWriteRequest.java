package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeContentWriteRequest {
    private final String text;
    private final String details;
    private final String note;
    private final List<AttributeEntry> attributes;
    private final List<String> tags;
    private final List<String> icons;

    @JsonCreator
    public NodeContentWriteRequest(@JsonProperty("text") String text,
                                   @JsonProperty("details") String details,
                                   @JsonProperty("note") String note,
                                   @JsonProperty("attributes") List<AttributeEntry> attributes,
                                   @JsonProperty("tags") List<String> tags,
                                   @JsonProperty("icons") List<String> icons) {
        this.text = text;
        this.details = details;
        this.note = note;
        this.attributes = attributes;
        this.tags = tags;
        this.icons = icons;
    }

    public String getText() {
        return text;
    }

    public String getDetails() {
        return details;
    }

    public String getNote() {
        return note;
    }

    public List<AttributeEntry> getAttributes() {
        return attributes;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getIcons() {
        return icons;
    }
}
