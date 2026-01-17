package org.freeplane.plugin.ai.tools.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchConditionDefinition {
    private final String name;
    private final String valueInputMode;
    private final boolean allowsCaseSensitiveOption;
    private final boolean allowsApproximateMatchingOption;
    private final boolean allowsIgnoreDiacriticsOption;

    @JsonCreator
    public SearchConditionDefinition(@JsonProperty("name") String name,
                                     @JsonProperty("valueInputMode") String valueInputMode,
                                     @JsonProperty("allowsCaseSensitiveOption") boolean allowsCaseSensitiveOption,
                                     @JsonProperty("allowsApproximateMatchingOption") boolean allowsApproximateMatchingOption,
                                     @JsonProperty("allowsIgnoreDiacriticsOption") boolean allowsIgnoreDiacriticsOption) {
        this.name = name;
        this.valueInputMode = valueInputMode;
        this.allowsCaseSensitiveOption = allowsCaseSensitiveOption;
        this.allowsApproximateMatchingOption = allowsApproximateMatchingOption;
        this.allowsIgnoreDiacriticsOption = allowsIgnoreDiacriticsOption;
    }

    public String getName() {
        return name;
    }

    public String getValueInputMode() {
        return valueInputMode;
    }

    public boolean allowsCaseSensitiveOption() {
        return allowsCaseSensitiveOption;
    }

    public boolean allowsApproximateMatchingOption() {
        return allowsApproximateMatchingOption;
    }

    public boolean allowsIgnoreDiacriticsOption() {
        return allowsIgnoreDiacriticsOption;
    }
}
