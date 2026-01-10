package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchConditionsForPropertyResponse {
    private final String propertyName;
    private final List<SearchConditionDefinition> conditions;

    @JsonCreator
    public SearchConditionsForPropertyResponse(@JsonProperty("propertyName") String propertyName,
                                               @JsonProperty("conditions") List<SearchConditionDefinition> conditions) {
        this.propertyName = propertyName;
        this.conditions = conditions;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public List<SearchConditionDefinition> getConditions() {
        return conditions;
    }
}
