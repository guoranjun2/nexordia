package org.freeplane.plugin.ai.chat.history;

public class ChatTranscriptEntry {
    private ChatTranscriptRole role;
    private String text;

    public ChatTranscriptEntry() {
    }

    public ChatTranscriptEntry(ChatTranscriptRole role, String text) {
        this.role = role;
        this.text = text;
    }

    public ChatTranscriptRole getRole() {
        return role;
    }

    public void setRole(ChatTranscriptRole role) {
        this.role = role;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
