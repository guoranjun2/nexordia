package org.freeplane.plugin.ai.tools;

public final class SearchConditionState {
    private final String propertyName;
    private final String conditionName;
    private final String value;
    private final boolean isCaseSensitive;
    private final boolean usesApproximateMatching;
    private final boolean ignoresDiacritics;

    public SearchConditionState(String propertyName, String conditionName, String value, boolean isCaseSensitive,
                                boolean usesApproximateMatching, boolean ignoresDiacritics) {
        this.propertyName = propertyName;
        this.conditionName = conditionName;
        this.value = value;
        this.isCaseSensitive = isCaseSensitive;
        this.usesApproximateMatching = usesApproximateMatching;
        this.ignoresDiacritics = ignoresDiacritics;
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
        return isCaseSensitive;
    }

    public boolean usesApproximateMatching() {
        return usesApproximateMatching;
    }

    public boolean ignoresDiacritics() {
        return ignoresDiacritics;
    }
}
