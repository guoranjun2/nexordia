package org.freeplane.plugin.ai.prompt;

public class AiPrompt {
    private String name;
    private String prompt;
    private boolean showInChat;
    private String modelSelectionValue;
    private String toolAvailabilitySelectionValue;

    public AiPrompt() {
        this("", "", false, "", "");
    }

    public AiPrompt(String name, String prompt, boolean showInChat) {
        this(name, prompt, showInChat, "", "");
    }

    public AiPrompt(String name, String prompt, boolean showInChat, String modelSelectionValue) {
        this(name, prompt, showInChat, modelSelectionValue, "");
    }

    public AiPrompt(String name, String prompt, boolean showInChat,
                    String modelSelectionValue,
                    String toolAvailabilitySelectionValue) {
        this.name = name;
        this.prompt = prompt;
        this.showInChat = showInChat;
        this.modelSelectionValue = modelSelectionValue;
        this.toolAvailabilitySelectionValue = toolAvailabilitySelectionValue;
    }

    public AiPrompt copy() {
        return new AiPrompt(name, prompt, showInChat, modelSelectionValue, toolAvailabilitySelectionValue);
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

    public String getModelSelectionValue() {
        return modelSelectionValue;
    }

    public void setModelSelectionValue(String modelSelectionValue) {
        this.modelSelectionValue = modelSelectionValue;
    }

    public String getToolAvailabilitySelectionValue() {
        return toolAvailabilitySelectionValue;
    }

    public void setToolAvailabilitySelectionValue(String toolAvailabilitySelectionValue) {
        this.toolAvailabilitySelectionValue = toolAvailabilitySelectionValue;
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
