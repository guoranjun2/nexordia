package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class SearchConditionsForPropertyResponse {
    private final String propertyName;
    private final List<SearchConditionDefinition> conditions;

    public SearchConditionsForPropertyResponse(String propertyName, List<SearchConditionDefinition> conditions) {
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
