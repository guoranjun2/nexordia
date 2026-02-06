package org.freeplane.plugin.ai.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

public class AssistantProfile {
    public static final String DEFAULT_ID = "default";
    public static final String CUSTOM_ID = "custom";

    private String id;
    private String name;
    private String prompt;

    public AssistantProfile() {
    }

    public AssistantProfile(String id, String name, String prompt) {
        this.id = id;
        this.name = name;
        this.prompt = prompt;
    }

    public static AssistantProfile defaultProfile() {
        return new AssistantProfile(DEFAULT_ID, "Default", "");
    }

    public static AssistantProfile customProfile(String prompt) {
        return new AssistantProfile(CUSTOM_ID, "Custom", prompt);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @JsonIgnore
    public boolean isDefault() {
        return Objects.equals(DEFAULT_ID, id);
    }

    @JsonIgnore
    public boolean isCustom() {
        return Objects.equals(CUSTOM_ID, id);
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
