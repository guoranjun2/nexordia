package org.freeplane.plugin.ai.tools;

public final class TextualContent {
    private final String text;
    private final String details;
    private final String note;

    public TextualContent(String text, String details, String note) {
        this.text = text;
        this.details = details;
        this.note = note;
    }

    public String getText() {
        return text;
    }

    public String getDetails() {
        return details;
    }

    public String getNote() {
        return note;
    }
}
