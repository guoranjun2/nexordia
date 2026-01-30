package org.freeplane.plugin.ai.chat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class LiveChatSession {
    private final LiveChatSessionId id;
    private final ChatSessionMemoryController chatMemoryController;
    private final Set<String> mapIds;
    private List<ChatMessageHistory.ChatMessageSnapshot> messageSnapshots;
    private String displayName;
    private boolean nameEdited;
    private boolean userMessageNameApplied;

    LiveChatSession(LiveChatSessionId id, ChatSessionMemoryController chatMemoryController, String displayName) {
        this.id = id;
        this.chatMemoryController = chatMemoryController;
        this.displayName = displayName;
        this.mapIds = new LinkedHashSet<>();
        this.messageSnapshots = new ArrayList<>();
    }

    LiveChatSessionId getId() {
        return id;
    }

    ChatSessionMemoryController getChatMemoryController() {
        return chatMemoryController;
    }

    List<ChatMessageHistory.ChatMessageSnapshot> getMessageSnapshots() {
        return messageSnapshots;
    }

    void setMessageSnapshots(List<ChatMessageHistory.ChatMessageSnapshot> messageSnapshots) {
        this.messageSnapshots = messageSnapshots;
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
}
