package org.freeplane.plugin.ai.tools;

public final class SearchConditionRequest {
    private final String propertyName;
    private final String conditionName;
    private final String value;
    private final boolean caseSensitive;
    private final boolean approximateMatching;
    private final boolean ignoreDiacritics;

    public SearchConditionRequest(String propertyName, String conditionName, String value, boolean caseSensitive,
                                  boolean approximateMatching, boolean ignoreDiacritics) {
        this.propertyName = propertyName;
        this.conditionName = conditionName;
        this.value = value;
        this.caseSensitive = caseSensitive;
        this.approximateMatching = approximateMatching;
        this.ignoreDiacritics = ignoreDiacritics;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getConditionName() {
        return conditionName;
    }

    public String getValue() {
        return value;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isApproximateMatching() {
        return approximateMatching;
    }

    public boolean isIgnoreDiacritics() {
        return ignoreDiacritics;
    }
}
