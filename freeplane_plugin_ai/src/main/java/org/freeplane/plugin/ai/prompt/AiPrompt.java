package org.freeplane.plugin.ai.prompt;

public class AiPrompt {
    private String name;
    private String prompt;
    private boolean showInChat;

    public AiPrompt() {
    }

    public AiPrompt(String name, String prompt, boolean showInChat) {
        this.name = name;
        this.prompt = prompt;
        this.showInChat = showInChat;
    }

    public AiPrompt copy() {
        return new AiPrompt(name, prompt, showInChat);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isShowInChat() {
        return showInChat;
    }

    public void setShowInChat(boolean showInChat) {
        this.showInChat = showInChat;
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
