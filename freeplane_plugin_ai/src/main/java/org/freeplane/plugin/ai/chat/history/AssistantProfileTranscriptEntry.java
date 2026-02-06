package org.freeplane.plugin.ai.chat.history;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AssistantProfileTranscriptEntry extends ChatTranscriptEntry {
    private String profileId;
    private String profileName;
    private boolean containsProfileDefinition;

    public AssistantProfileTranscriptEntry() {
        setRole(ChatTranscriptRole.ASSISTANT_PROFILE_SYSTEM);
    }

    public AssistantProfileTranscriptEntry(String profileId,
                                           String profileName,
                                           boolean containsProfileDefinition) {
        this();
        this.profileId = profileId;
        this.profileName = profileName;
        this.containsProfileDefinition = containsProfileDefinition;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @JsonProperty("containsProfileDefinition")
    public boolean containsProfileDefinition() {
        return containsProfileDefinition;
    }

    @JsonProperty("containsProfileDefinition")
    public void setContainsProfileDefinition(boolean containsProfileDefinition) {
        this.containsProfileDefinition = containsProfileDefinition;
    }
}
