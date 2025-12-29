package org.freeplane.plugin.ai.tools;

public final class AttributesContentRequest {
    private final boolean includesAttributes;

    public AttributesContentRequest(boolean includesAttributes) {
        this.includesAttributes = includesAttributes;
    }

    public boolean includesAttributes() {
        return includesAttributes;
    }
}
