package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditableAttribute {
    private final String name;
    private final String rawValue;
    private final String transformedValue;
    private final String plainValue;
    private final Boolean hasMarkup;
    private final Boolean isFormula;
    private final Integer index;

    @JsonCreator
    public EditableAttribute(@JsonProperty("name") String name,
                             @JsonProperty("rawValue") String rawValue,
                             @JsonProperty("transformedValue") String transformedValue,
                             @JsonProperty("plainValue") String plainValue,
                             @JsonProperty("hasMarkup") Boolean hasMarkup,
                             @JsonProperty("isFormula") Boolean isFormula,
                             @JsonProperty("index") Integer index) {
        this.name = name;
        this.rawValue = rawValue;
        this.transformedValue = transformedValue;
        this.plainValue = plainValue;
        this.hasMarkup = hasMarkup;
        this.isFormula = isFormula;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getTransformedValue() {
        return transformedValue;
    }

    public String getPlainValue() {
        return plainValue;
    }

    public Boolean getHasMarkup() {
        return hasMarkup;
    }

    public Boolean getIsFormula() {
        return isFormula;
    }

    public Integer getIndex() {
        return index;
    }
}
