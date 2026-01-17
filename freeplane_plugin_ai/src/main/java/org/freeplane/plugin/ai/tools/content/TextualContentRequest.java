package org.freeplane.plugin.ai.tools.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TextualContentRequest {
    private final boolean includesText;
    private final boolean includesDetails;
    private final boolean includesNote;

    @JsonCreator
    public TextualContentRequest(@JsonProperty("includesText") boolean includesText,
                                 @JsonProperty("includesDetails") boolean includesDetails,
                                 @JsonProperty("includesNote") boolean includesNote) {
        this.includesText = includesText;
        this.includesDetails = includesDetails;
        this.includesNote = includesNote;
    }

    public boolean includesText() {
        return includesText;
    }

    public boolean includesDetails() {
        return includesDetails;
    }

    public boolean includesNote() {
        return includesNote;
    }
}
