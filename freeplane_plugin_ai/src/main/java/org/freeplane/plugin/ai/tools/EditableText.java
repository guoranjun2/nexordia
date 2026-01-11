package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditableText {
    private final String raw;
    private final String transformed;
    private final String plain;
    private final ContentType contentType;
    private final Boolean hasMarkup;
    private final Boolean isFormula;

    @JsonCreator
    public EditableText(@JsonProperty("raw") String raw,
                        @JsonProperty("transformed") String transformed,
                        @JsonProperty("plain") String plain,
                        @JsonProperty("contentType") ContentType contentType,
                        @JsonProperty("hasMarkup") Boolean hasMarkup,
                        @JsonProperty("isFormula") Boolean isFormula) {
        this.raw = raw;
        this.transformed = transformed;
        this.plain = plain;
        this.contentType = contentType;
        this.hasMarkup = hasMarkup;
        this.isFormula = isFormula;
    }

    public String getRaw() {
        return raw;
    }

    public String getTransformed() {
        return transformed;
    }

    public String getPlain() {
        return plain;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public Boolean getHasMarkup() {
        return hasMarkup;
    }

    public Boolean getIsFormula() {
        return isFormula;
    }
}
