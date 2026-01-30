package org.freeplane.plugin.ai.chat;

import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.maps.AvailableMaps;

import org.freeplane.features.map.MapModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LiveChatController {

    public interface SessionActivationHandler {
        void activate(ChatSessionMemoryController sessionMemoryController);
    }

    private final AIChatPanel owner;
    private final AvailableMaps availableMaps;
    private final TextController textController;
    private final LiveChatSessionManager liveChatSessionManager;
    private final ChatMessageHistory messageHistory;
    private final DateTimeFormatter chatNameFormatter;
    private final Runnable persistCurrentSession;
    private final SessionActivationHandler sessionActivationHandler;

    public LiveChatController(AIChatPanel parent,
                              ChatMessageHistory messageHistory,
                              AvailableMaps availableMaps,
                              TextController textController,
                              DateTimeFormatter chatNameFormatter,
                              Runnable persistCurrentSession,
                              SessionActivationHandler sessionActivationHandler) {
        this.owner = parent;
        this.messageHistory = messageHistory;
        this.availableMaps = availableMaps;
        this.textController = textController;
        this.chatNameFormatter = chatNameFormatter;
        this.persistCurrentSession = persistCurrentSession;
        this.sessionActivationHandler = sessionActivationHandler;
        this.liveChatSessionManager = new LiveChatSessionManager();
    }

    public void initialize(ChatSessionMemoryController sessionMemoryController) {
        LiveChatSession initialSession = liveChatSessionManager.createSession(
            sessionMemoryController, buildDefaultChatName());
        liveChatSessionManager.setCurrentSession(initialSession.getId());
        sessionActivationHandler.activate(sessionMemoryController);
    }

    public void startNewChat() {
        switchToNewSession();
    }

    public void openLiveChats() {
        saveCurrentSessionState();
        createLiveChatListDialog().openDialog();
    }

    public void updateSessionNameFromFirstUserMessage(String userMessage) {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        if (session.isNameEdited() || session.isUserMessageNameApplied()) {
            return;
        }
        String normalized = userMessage == null ? "" : userMessage.trim();
        if (normalized.isEmpty()) {
            return;
        }
        String updatedName = buildUserMessageName(session.getDisplayName(), normalized);
        liveChatSessionManager.updateUserMessageName(updatedName);
    }

    public AvailableMaps.MapAccessListener mapAccessListener() {
        return this::recordMapAccess;
    }

    private void switchToNewSession() {
        saveCurrentSessionState();
        persistCurrentSession.run();
        ChatSessionMemoryController newChatMemory = new ChatSessionMemoryController();
        LiveChatSession newSession = liveChatSessionManager.createSession(newChatMemory, buildDefaultChatName());
        switchToSession(newSession.getId(), false);
    }

    private void switchToSession(LiveChatSessionId sessionId) {
        switchToSession(sessionId, true);
    }

    private void switchToSession(LiveChatSessionId sessionId, boolean saveCurrent) {
        if (sessionId == null) {
            return;
        }
        if (saveCurrent) {
            saveCurrentSessionState();
            persistCurrentSession.run();
        }
        liveChatSessionManager.setCurrentSession(sessionId);
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        messageHistory.restoreMessages(session.getMessageSnapshots());
        sessionActivationHandler.activate(session.getChatMemoryController());
    }

    private void closeSession(LiveChatSessionId sessionId) {
        if (sessionId == null) {
            return;
        }
        LiveChatSession activeSession = liveChatSessionManager.getCurrentSession();
        if (activeSession != null && sessionId.equals(activeSession.getId())) {
            saveCurrentSessionState();
            persistCurrentSession.run();
        }
        liveChatSessionManager.remove(sessionId);
        LiveChatSession nextSession = liveChatSessionManager.getCurrentSession();
        if (nextSession == null) {
            switchToNewSession();
            return;
        }
        switchToSession(nextSession.getId(), false);
    }

    private void saveCurrentSessionState() {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        List<ChatMessageHistory.ChatMessageSnapshot> snapshots = messageHistory.snapshot();
        session.setMessageSnapshots(new ArrayList<>(snapshots));
    }

    private String buildDefaultChatName() {
        return chatNameFormatter.format(LocalDateTime.now());
    }

    private String buildUserMessageName(String timestampLabel, String userMessage) {
        String[] words = userMessage.split("\\s+");
        StringBuilder builder = new StringBuilder(timestampLabel);
        builder.append(" - ");
        for (int index = 0; index < words.length && index < 4; index++) {
            if (index > 0) {
                builder.append(' ');
            }
            builder.append(words[index]);
        }
        return builder.toString().trim();
    }

    private void recordMapAccess(UUID mapIdentifier, MapModel mapModel) {
        if (mapIdentifier == null) {
            return;
        }
        liveChatSessionManager.recordMapId(mapIdentifier.toString());
    }

    private LiveChatListDialog createLiveChatListDialog() {
        return new LiveChatListDialog(
            owner,
            liveChatSessionManager,
            availableMaps,
            textController,
            new LiveChatListDialog.LiveChatListHandler() {
                @Override
                public void switchTo(LiveChatSessionId sessionId) {
                    switchToSession(sessionId);
                }

                @Override
                public void close(LiveChatSessionId sessionId) {
                    closeSession(sessionId);
                }

                @Override
                public void rename(LiveChatSessionId sessionId, String displayName) {
                    liveChatSessionManager.rename(sessionId, displayName);
                }
            }
        );
    }
}
