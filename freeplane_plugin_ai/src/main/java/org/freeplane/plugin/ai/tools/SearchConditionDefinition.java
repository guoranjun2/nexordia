package org.freeplane.plugin.ai.tools;

public final class SearchConditionDefinition {
    private final String name;
    private final String valueInputMode;
    private final boolean caseSensitiveOptionAllowed;
    private final boolean approximateMatchingOptionAllowed;
    private final boolean ignoreDiacriticsOptionAllowed;

    public SearchConditionDefinition(String name, String valueInputMode, boolean caseSensitiveOptionAllowed,
                                     boolean approximateMatchingOptionAllowed, boolean ignoreDiacriticsOptionAllowed) {
        this.name = name;
        this.valueInputMode = valueInputMode;
        this.caseSensitiveOptionAllowed = caseSensitiveOptionAllowed;
        this.approximateMatchingOptionAllowed = approximateMatchingOptionAllowed;
        this.ignoreDiacriticsOptionAllowed = ignoreDiacriticsOptionAllowed;
    }

    public String getName() {
        return name;
    }

    public String getValueInputMode() {
        return valueInputMode;
    }

    public boolean isCaseSensitiveOptionAllowed() {
        return caseSensitiveOptionAllowed;
    }

    public boolean isApproximateMatchingOptionAllowed() {
        return approximateMatchingOptionAllowed;
    }

    public boolean isIgnoreDiacriticsOptionAllowed() {
        return ignoreDiacriticsOptionAllowed;
    }
}
