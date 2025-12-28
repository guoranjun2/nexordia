package org.freeplane.plugin.ai.tools;

public final class SearchConditionsForPropertyRequest {
    private final String propertyName;

    public SearchConditionsForPropertyRequest(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
