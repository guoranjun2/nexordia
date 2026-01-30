package org.freeplane.plugin.ai.chat;

import java.util.ArrayList;
import java.util.List;

final class LiveChatSessionSummary {
    private final LiveChatSessionId id;
    private final String displayName;
    private final List<String> mapIds;

    LiveChatSessionSummary(LiveChatSessionId id, String displayName, List<String> mapIds) {
        this.id = id;
        this.displayName = displayName;
        this.mapIds = mapIds == null ? new ArrayList<>() : new ArrayList<>(mapIds);
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
}
