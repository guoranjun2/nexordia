package org.freeplane.plugin.ai.chat;

import java.util.ArrayList;
import java.util.List;

import org.freeplane.plugin.ai.chat.history.ChatTranscriptId;

final class LiveChatSessionSummary {
    private final LiveChatSessionId id;
    private final String displayName;
    private final List<String> mapIds;
    private final ChatTranscriptId transcriptId;
    private final long lastActivityTimestamp;

    LiveChatSessionSummary(LiveChatSessionId id, String displayName, List<String> mapIds,
                           ChatTranscriptId transcriptId, long lastActivityTimestamp) {
        this.id = id;
        this.displayName = displayName;
        this.mapIds = mapIds == null ? new ArrayList<>() : new ArrayList<>(mapIds);
        this.transcriptId = transcriptId;
        this.lastActivityTimestamp = lastActivityTimestamp;
    }

    LiveChatSessionId getId() {
        return id;
    }

    String getDisplayName() {
        return displayName;
    }

    List<String> getMapIds() {
        return new ArrayList<>(mapIds);
    }

    ChatTranscriptId getTranscriptId() {
        return transcriptId;
    }

    long getLastActivityTimestamp() {
        return lastActivityTimestamp;
    }

}
