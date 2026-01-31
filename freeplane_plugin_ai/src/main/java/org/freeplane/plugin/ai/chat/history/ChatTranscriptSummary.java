package org.freeplane.plugin.ai.chat.history;

import java.util.ArrayList;
import java.util.List;

public class ChatTranscriptSummary {
    private ChatTranscriptId id;
    private long timestamp;
    private String displayName;
    private List<MapRootShortTextCount> mapRootShortTextCounts = new ArrayList<>();
    private ChatTranscriptStatus status = ChatTranscriptStatus.TRANSCRIPT;
    private String errorMessage;

    public ChatTranscriptSummary() {
    }

    public ChatTranscriptId getId() {
        return id;
    }

    public void setId(ChatTranscriptId id) {
        this.id = id;
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

    public ChatTranscriptStatus getStatus() {
        return status;
    }

    public void setStatus(ChatTranscriptStatus status) {
        this.status = status == null ? ChatTranscriptStatus.TRANSCRIPT : status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
