package org.freeplane.plugin.ai.chat.history;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

public class ChatTranscriptRecord {
    private long timestamp;
    private String displayName;
    private Boolean assistantProfileEnabled;
    private String selectedModelOverride;
    private String toolAvailabilityOverride;
    private boolean toolAvailabilityOverrideMetadata;
    private List<MapRootShortTextCount> mapRootShortTextCounts = new ArrayList<>();
    private List<ChatTranscriptEntry> entries = new ArrayList<>();

    public ChatTranscriptRecord() {
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getAssistantProfileEnabled() {
        return assistantProfileEnabled;
    }

    public void setAssistantProfileEnabled(Boolean assistantProfileEnabled) {
        this.assistantProfileEnabled = assistantProfileEnabled;
    }

    public String getSelectedModelOverride() {
        return selectedModelOverride;
    }

    public void setSelectedModelOverride(String selectedModelOverride) {
        this.selectedModelOverride = selectedModelOverride;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getToolAvailabilityOverride() {
        return toolAvailabilityOverride;
    }

    public void setToolAvailabilityOverride(String toolAvailabilityOverride) {
        this.toolAvailabilityOverride = toolAvailabilityOverride;
        this.toolAvailabilityOverrideMetadata = true;
    }

    public boolean isToolAvailabilityOverrideMetadata() {
        return toolAvailabilityOverrideMetadata;
    }

    public void setToolAvailabilityOverrideMetadata(boolean toolAvailabilityOverrideMetadata) {
        this.toolAvailabilityOverrideMetadata = toolAvailabilityOverrideMetadata;
    }

    @JsonIgnore
    public boolean hasToolAvailabilityOverrideMetadata() {
        return toolAvailabilityOverrideMetadata;
    }

    public List<MapRootShortTextCount> getMapRootShortTextCounts() {
        return mapRootShortTextCounts;
    }

    public void setMapRootShortTextCounts(List<MapRootShortTextCount> mapRootShortTextCounts) {
        this.mapRootShortTextCounts = mapRootShortTextCounts == null ? new ArrayList<>() : mapRootShortTextCounts;
    }

    public List<ChatTranscriptEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<ChatTranscriptEntry> entries) {
        this.entries = entries == null ? new ArrayList<>() : entries;
    }
}
