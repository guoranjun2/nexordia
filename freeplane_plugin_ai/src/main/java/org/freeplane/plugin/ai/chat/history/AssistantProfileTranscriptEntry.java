package org.freeplane.plugin.ai.chat.history;

public class AssistantProfileTranscriptEntry extends ChatTranscriptEntry {
    private String profileName;
    private String profileDefinition;
    private boolean historicalMarker;

    public AssistantProfileTranscriptEntry() {
        setRole(ChatTranscriptRole.ASSISTANT_PROFILE_SYSTEM);
    }

    public AssistantProfileTranscriptEntry(String profileName, String profileDefinition, boolean historicalMarker) {
        this();
        this.profileName = profileName;
        this.profileDefinition = profileDefinition;
        this.historicalMarker = historicalMarker;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getProfileDefinition() {
        return profileDefinition;
    }

    public void setProfileDefinition(String profileDefinition) {
        this.profileDefinition = profileDefinition;
    }

    public boolean isHistoricalMarker() {
        return historicalMarker;
    }

    public void setHistoricalMarker(boolean historicalMarker) {
        this.historicalMarker = historicalMarker;
    }
}
