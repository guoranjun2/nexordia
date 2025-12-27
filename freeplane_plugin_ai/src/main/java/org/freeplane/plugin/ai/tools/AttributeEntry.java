package org.freeplane.plugin.ai.tools;

public final class AttributeEntry {
    private final String name;
    private final String value;

    public AttributeEntry(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
