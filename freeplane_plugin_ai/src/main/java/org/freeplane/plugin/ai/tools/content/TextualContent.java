package org.freeplane.plugin.ai.tools.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextualContent {
    private final String text;
    private final String details;
    private final String note;

    @JsonCreator
    public TextualContent(@JsonProperty("text") String text,
                          @JsonProperty("details") String details,
                          @JsonProperty("note") String note) {
        this.text = text;
        this.details = details;
        this.note = note;
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
}
