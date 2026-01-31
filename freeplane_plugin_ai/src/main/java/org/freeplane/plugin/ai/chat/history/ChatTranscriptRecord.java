package org.freeplane.plugin.ai.chat.history;

import java.util.ArrayList;
import java.util.List;

public class ChatTranscriptRecord {
    private long timestamp;
    private String displayName;
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
