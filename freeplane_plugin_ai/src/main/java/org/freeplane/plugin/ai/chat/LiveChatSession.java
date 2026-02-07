package org.freeplane.plugin.ai.chat;

import dev.langchain4j.memory.ChatMemory;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptId;
import org.freeplane.plugin.ai.chat.history.MapRootShortTextCount;

final class LiveChatSession {
    private final LiveChatSessionId id;
    private final ChatMemory chatMemory;
    private final Set<String> mapIds;
    private final List<MapRootShortTextCount> mapRootShortTextCounts;
    private List<ChatTranscriptEntry> transcriptEntries;
    private ChatTranscriptId transcriptId;
    private String displayName;
    private boolean nameEdited;
    private boolean userMessageNameApplied;
    private long lastActivityTimestamp;
    private ChatTokenUsageState tokenUsageState;

    LiveChatSession(LiveChatSessionId id, ChatMemory chatMemory, String displayName) {
        this.id = id;
        this.chatMemory = chatMemory;
        this.displayName = displayName;
        this.mapIds = new LinkedHashSet<>();
        this.mapRootShortTextCounts = new ArrayList<>();
        this.transcriptEntries = new ArrayList<>();
    }

    LiveChatSessionId getId() {
        return id;
    }

    ChatMemory getChatMemory() {
        return chatMemory;
    }

    List<ChatTranscriptEntry> getTranscriptEntries() {
        return transcriptEntries;
    }

    void setTranscriptEntries(List<ChatTranscriptEntry> transcriptEntries) {
        this.transcriptEntries = transcriptEntries;
    }

    ChatTranscriptId getTranscriptId() {
        return transcriptId;
    }

    void setTranscriptId(ChatTranscriptId transcriptId) {
        this.transcriptId = transcriptId;
    }

    String getDisplayName() {
        return displayName;
    }

    void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    boolean isNameEdited() {
        return nameEdited;
    }

    void setNameEdited(boolean nameEdited) {
        this.nameEdited = nameEdited;
    }

    boolean isUserMessageNameApplied() {
        return userMessageNameApplied;
    }

    void setUserMessageNameApplied(boolean userMessageNameApplied) {
        this.userMessageNameApplied = userMessageNameApplied;
    }

    Set<String> getMapIds() {
        return mapIds;
    }

    List<MapRootShortTextCount> getMapRootShortTextCounts() {
        return mapRootShortTextCounts;
    }

    void setMapRootShortTextCounts(List<MapRootShortTextCount> mapRootShortTextCounts) {
        this.mapRootShortTextCounts.clear();
        if (mapRootShortTextCounts != null) {
            this.mapRootShortTextCounts.addAll(mapRootShortTextCounts);
        }
    }

    long getLastActivityTimestamp() {
        return lastActivityTimestamp;
    }

    void setLastActivityTimestamp(long lastActivityTimestamp) {
        this.lastActivityTimestamp = lastActivityTimestamp;
    }

    ChatTokenUsageState getTokenUsageState() {
        return tokenUsageState;
    }

    void setTokenUsageState(ChatTokenUsageState tokenUsageState) {
        this.tokenUsageState = tokenUsageState;
    }

}
