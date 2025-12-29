package org.freeplane.plugin.ai.tools;

public final class SearchConditionDefinition {
    private final String name;
    private final String valueInputMode;
    private final boolean allowsCaseSensitiveOption;
    private final boolean allowsApproximateMatchingOption;
    private final boolean allowsIgnoreDiacriticsOption;

    public SearchConditionDefinition(String name, String valueInputMode, boolean allowsCaseSensitiveOption,
                                     boolean allowsApproximateMatchingOption, boolean allowsIgnoreDiacriticsOption) {
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
