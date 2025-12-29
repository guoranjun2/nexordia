package org.freeplane.plugin.ai.tools;

public final class TextualContentRequest {
    private final boolean includesText;
    private final boolean includesDetails;
    private final boolean includesNote;

    public TextualContentRequest(boolean includesText, boolean includesDetails, boolean includesNote) {
        this.includesText = includesText;
        this.includesDetails = includesDetails;
        this.includesNote = includesNote;
    }

    public boolean includesText() {
        return includesText;
    }

    public boolean includesDetails() {
        return includesDetails;
    }

    public boolean includesNote() {
        return includesNote;
    }
}
