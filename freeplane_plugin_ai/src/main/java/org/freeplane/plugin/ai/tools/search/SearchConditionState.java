package org.freeplane.plugin.ai.tools.search;

import java.util.Objects;

public class SearchConditionState {
    private final String propertyName;
    private final String conditionName;
    private final String value;
    private final boolean isCaseSensitive;
    private final boolean usesApproximateMatching;
    private final boolean ignoresDiacritics;

    public SearchConditionState(SearchConditionRequest request) {
        Objects.requireNonNull(request, "request");
        this.propertyName = request.getPropertyName();
        this.conditionName = request.getConditionName();
        this.value = request.getValue();
        this.isCaseSensitive = request.isCaseSensitive();
        this.usesApproximateMatching = request.usesApproximateMatching();
        this.ignoresDiacritics = request.ignoresDiacritics();
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
