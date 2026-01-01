package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SearchConditionsForPropertyRequest {
    private final String propertyName;

    @JsonCreator
    public SearchConditionsForPropertyRequest(@JsonProperty("propertyName") String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
