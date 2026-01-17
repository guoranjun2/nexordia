package org.freeplane.plugin.ai.tools.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchConditionRequest {
    private final String propertyName;
    private final String conditionName;
    private final String value;
    private final boolean isCaseSensitive;
    private final boolean usesApproximateMatching;
    private final boolean ignoresDiacritics;

    @JsonCreator
    public SearchConditionRequest(@JsonProperty("propertyName") String propertyName,
                                  @JsonProperty("conditionName") String conditionName,
                                  @JsonProperty("value") String value,
                                  @JsonProperty("isCaseSensitive") boolean isCaseSensitive,
                                  @JsonProperty("usesApproximateMatching") boolean usesApproximateMatching,
                                  @JsonProperty("ignoresDiacritics") boolean ignoresDiacritics) {
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
