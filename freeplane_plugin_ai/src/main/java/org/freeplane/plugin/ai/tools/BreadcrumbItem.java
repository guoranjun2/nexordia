package org.freeplane.plugin.ai.tools;

public final class BreadcrumbItem {
    private final String text;
    private final String nodeIdentifier;

    public BreadcrumbItem(String text, String nodeIdentifier) {
        this.text = text;
        this.nodeIdentifier = nodeIdentifier;
    }

    public String getText() {
        return text;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }
}
