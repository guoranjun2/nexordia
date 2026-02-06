package org.freeplane.plugin.ai.chat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.text.TextController;
import org.freeplane.core.util.TextUtils;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.AssistantProfileTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptId;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRecord;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRole;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptStore;
import org.freeplane.plugin.ai.chat.history.MapRootShortTextCount;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class LiveChatController {

    public interface SessionActivationHandler {
        void activate(ChatSessionMemoryController sessionMemoryController);
    }

    private final AIChatPanel owner;
    private final LiveChatSessionManager liveChatSessionManager;
    private final ChatMessageHistory messageHistory;
    private final DateTimeFormatter chatNameFormatter;
    private final SessionActivationHandler sessionActivationHandler;
    private final ChatTranscriptStore transcriptStore;
    private final LiveTranscriptAdapter transcriptAdapter;
    private final MapRootShortTextFormatter mapRootShortTextFormatter;
    private final MapRootShortTextCountsMerger mapRootShortTextCountsMerger;
    private ChatTranscriptId loadedTranscriptId;
    private static final String USER_STYLE_CLASS = "message-user";
    private static final String ASSISTANT_STYLE_CLASS = "message-assistant";
    private static final String SYSTEM_STYLE_CLASS = "message-system";
    private static final String PROFILE_STYLE_CLASS = "message-profile";
    private static final String TRANSCRIPT_HIDDEN_SYSTEM_MESSAGE =
        "System message: The messages in this session include a restored transcript of a prior chat. "
            + "Treat those messages as the earlier conversation context, not as hallucinations. "
            + "The currently opened map may differ from the maps discussed in that transcript. "
            + "Confirm the map context with the user when needed. The real conversation begins after this message. ";

    public LiveChatController(AIChatPanel parent,
                              ChatMessageHistory messageHistory,
                              AvailableMaps availableMaps,
                              TextController textController,
                              DateTimeFormatter chatNameFormatter,
                              SessionActivationHandler sessionActivationHandler) {
        this.owner = parent;
        this.messageHistory = messageHistory;
        this.chatNameFormatter = chatNameFormatter;
        this.sessionActivationHandler = sessionActivationHandler;
        this.liveChatSessionManager = new LiveChatSessionManager();
        this.transcriptStore = new ChatTranscriptStore();
        this.transcriptAdapter = new LiveTranscriptAdapter();
        this.mapRootShortTextFormatter = new MapRootShortTextFormatter(availableMaps, textController);
        this.mapRootShortTextCountsMerger = new MapRootShortTextCountsMerger();
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
        createChatListDialog().openDialog();
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

    public void recordUserMessage(String message) {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        loadedTranscriptId = null;
        session.setLastActivityTimestamp(System.currentTimeMillis());
        transcriptAdapter.appendUserMessage(session, message);
    }

    public void recordAssistantMessage(String message) {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        loadedTranscriptId = null;
        session.setLastActivityTimestamp(System.currentTimeMillis());
        transcriptAdapter.appendAssistantMessage(session, message);
    }

    public void recordAssistantProfileMessage(AssistantProfileSystemMessage message) {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null || message == null) {
            return;
        }
        loadedTranscriptId = null;
        session.setLastActivityTimestamp(System.currentTimeMillis());
        transcriptAdapter.appendAssistantProfileMessage(session, message);
    }

    public List<ChatTranscriptEntry> snapshotTranscriptEntries() {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(session.getTranscriptEntries());
    }

    public void restoreTranscriptEntries(List<ChatTranscriptEntry> entries) {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        session.setTranscriptEntries(entries == null ? new ArrayList<>() : new ArrayList<>(entries));
    }

    public void persistCurrentSessionIfNeeded() {
        saveCurrentSessionState();
        persistCurrentSession();
    }

    private void switchToNewSession() {
        saveCurrentSessionState();
        persistCurrentSession();
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
            persistCurrentSession();
        }
        liveChatSessionManager.setCurrentSession(sessionId);
        loadedTranscriptId = null;
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
            persistCurrentSession();
        }
        liveChatSessionManager.remove(sessionId);
        LiveChatSession nextSession = liveChatSessionManager.getCurrentSession();
        if (nextSession == null) {
            switchToNewSession();
            return;
        }
        switchToSession(nextSession.getId(), false);
    }

    private void deleteLiveSessionInternal(LiveChatSessionId sessionId) {
        if (sessionId == null) {
            return;
        }
        LiveChatSession activeSession = liveChatSessionManager.getCurrentSession();
        boolean isActive = activeSession != null && sessionId.equals(activeSession.getId());
        liveChatSessionManager.remove(sessionId);
        if (!isActive) {
            return;
        }
        ChatSessionMemoryController newChatMemory = new ChatSessionMemoryController();
        LiveChatSession newSession = liveChatSessionManager.createSession(newChatMemory, buildDefaultChatName());
        liveChatSessionManager.setCurrentSession(newSession.getId());
        messageHistory.clear();
        sessionActivationHandler.activate(newChatMemory);
        loadedTranscriptId = null;
    }

    private void saveCurrentSessionState() {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        List<ChatMessageHistory.ChatMessageSnapshot> snapshots = messageHistory.snapshot();
        session.setMessageSnapshots(new ArrayList<>(snapshots));
    }

    private void persistCurrentSession() {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        if (session.getTranscriptEntries().isEmpty() && session.getTranscriptId() == null) {
            return;
        }
        ChatTranscriptRecord record = new ChatTranscriptRecord();
        record.setDisplayName(session.getDisplayName());
        record.setEntries(new ArrayList<>(session.getTranscriptEntries()));
        List<MapRootShortTextCount> currentCounts = mapRootShortTextFormatter.buildCounts(
            new ArrayList<>(session.getMapIds()));
        List<MapRootShortTextCount> mergedSessionCounts = mapRootShortTextCountsMerger.mergeByMax(
            session.getMapRootShortTextCounts(), currentCounts);
        List<MapRootShortTextCount> mergedCounts = mergedSessionCounts;
        if (session.getTranscriptId() != null) {
            ChatTranscriptRecord existingRecord = transcriptStore.load(session.getTranscriptId());
            if (existingRecord != null) {
                mergedCounts = mapRootShortTextCountsMerger.mergeByMax(
                    existingRecord.getMapRootShortTextCounts(), mergedSessionCounts);
            }
        }
        record.setMapRootShortTextCounts(mergedCounts);
        ChatTranscriptId transcriptId = transcriptStore.save(record, session.getTranscriptId());
        session.setTranscriptId(transcriptId);
        session.setLastActivityTimestamp(record.getTimestamp());
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

    private void recordMapAccess(UUID mapIdentifier, @SuppressWarnings("unused") MapModel mapModel) {
        if (mapIdentifier == null) {
            return;
        }
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        session.getMapIds().add(mapIdentifier.toString());
    }

    private ChatListDialog createChatListDialog() {
        return new ChatListDialog(
            owner,
            liveChatSessionManager,
            transcriptStore,
            mapRootShortTextFormatter,
            this::getLoadedTranscriptId,
            new ChatListDialog.ChatListHandler() {
                @Override
                public void switchTo(LiveChatSessionId sessionId) {
                    switchToSession(sessionId);
                }

                @Override
                public void close(LiveChatSessionId sessionId) {
                    closeSession(sessionId);
                }

                @Override
                public void deleteLiveSession(LiveChatSessionId sessionId) {
                    deleteLiveSessionInternal(sessionId);
                }

                @Override
                public void rename(LiveChatSessionId sessionId, String displayName) {
                    liveChatSessionManager.rename(sessionId, displayName);
                }

                @Override
                public void renameTranscript(ChatTranscriptId transcriptId, String displayName) {
                    transcriptStore.rename(transcriptId, displayName);
                }

                @Override
                public void startChatFromTranscript(ChatTranscriptId transcriptId) {
                    startChatFromTranscriptInternal(transcriptId);
                }

                @Override
                public void deleteTranscript(ChatTranscriptId transcriptId) {
                    transcriptStore.delete(transcriptId);
                }
            }
        );
    }

    private ChatTranscriptId getLoadedTranscriptId() {
        return loadedTranscriptId;
    }

    private void startChatFromTranscriptInternal(ChatTranscriptId transcriptId) {
        if (transcriptId == null) {
            return;
        }
        saveCurrentSessionState();
        persistCurrentSession();
        ChatTranscriptRecord record = transcriptStore.load(transcriptId);
        if (record == null) {
            return;
        }
        ChatSessionMemoryController newChatMemory = new ChatSessionMemoryController();
        LiveChatSession newSession = liveChatSessionManager.createSession(newChatMemory,
            record.getDisplayName() == null || record.getDisplayName().trim().isEmpty()
                ? buildDefaultChatName()
                : record.getDisplayName());
        newSession.setTranscriptId(transcriptId);
        newSession.setLastActivityTimestamp(record.getTimestamp());
        newSession.setMessageSnapshots(buildSnapshotsFromRecord(record));
        newSession.setMapRootShortTextCounts(record.getMapRootShortTextCounts());
        transcriptAdapter.setEntries(newSession, record.getEntries());
        switchToSession(newSession.getId(), false);
        loadedTranscriptId = transcriptId;
        seedTranscriptMemory(newSession, record);
    }

    private void seedTranscriptMemory(LiveChatSession session, ChatTranscriptRecord record) {
        if (session == null || record == null) {
            return;
        }
        session.getChatMemoryController().seedTranscriptWithHiddenExchange(record.getEntries(),
            TRANSCRIPT_HIDDEN_SYSTEM_MESSAGE);
    }

    private List<ChatMessageHistory.ChatMessageSnapshot> buildSnapshotsFromRecord(ChatTranscriptRecord record) {
        List<ChatMessageHistory.ChatMessageSnapshot> snapshots = new ArrayList<>();
        if (record == null || record.getEntries() == null) {
            return snapshots;
        }
        ChatMessageRenderer renderer = new ChatMessageRenderer();
        List<ChatTranscriptEntry> entries = record.getEntries();
        for (int index = 0; index < entries.size(); index++) {
            ChatTranscriptEntry entry = entries.get(index);
            if (entry == null || entry.getRole() == null) {
                continue;
            }
            if (isProfileAcknowledgementEntry(entries, index)) {
                continue;
            }
            addSnapshot(snapshots, renderer, entry);
        }
        return snapshots;
    }

    private void addSnapshot(List<ChatMessageHistory.ChatMessageSnapshot> snapshots,
                             ChatMessageRenderer renderer,
                             ChatTranscriptEntry entry) {
        if (entry == null || snapshots == null || renderer == null) {
            return;
        }
        if (entry.getRole() != ChatTranscriptRole.ASSISTANT_PROFILE_SYSTEM
            && (entry.getText() == null || entry.getText().trim().isEmpty())) {
            return;
        }
        boolean isAssistant = entry.getRole() == ChatTranscriptRole.ASSISTANT;
        String snapshotText = entry.getText();
        if (entry.getRole() == ChatTranscriptRole.ASSISTANT_PROFILE_SYSTEM) {
            snapshotText = buildProfilePaneMessage(entry);
        } else if (!isAssistant && entry.getRole() != ChatTranscriptRole.USER) {
            snapshotText = MessageBuilder.buildSystemInstructionText(snapshotText);
        }
        String messageText = renderer.renderMessage(snapshotText, isAssistant);
        String styleClassName;
        if (entry.getRole() == ChatTranscriptRole.USER) {
            styleClassName = USER_STYLE_CLASS;
        } else if (entry.getRole() == ChatTranscriptRole.ASSISTANT) {
            styleClassName = ASSISTANT_STYLE_CLASS;
        } else if (entry.getRole() == ChatTranscriptRole.ASSISTANT_PROFILE_SYSTEM) {
            styleClassName = PROFILE_STYLE_CLASS;
        } else {
            styleClassName = SYSTEM_STYLE_CLASS;
        }
        String messageMarkup = "<div class=\"" + styleClassName + "\">" + messageText + "</div>";
        snapshots.add(new ChatMessageHistory.ChatMessageSnapshot(snapshotText, messageMarkup, styleClassName));
    }

    private boolean isProfileAcknowledgementEntry(List<ChatTranscriptEntry> entries, int index) {
        if (entries == null || index <= 0 || index >= entries.size()) {
            return false;
        }
        ChatTranscriptEntry current = entries.get(index);
        ChatTranscriptEntry previous = entries.get(index - 1);
        if (current == null || previous == null) {
            return false;
        }
        if (current.getRole() != ChatTranscriptRole.ASSISTANT) {
            return false;
        }
        if (previous.getRole() != ChatTranscriptRole.ASSISTANT_PROFILE_SYSTEM) {
            return false;
        }
        String text = current.getText();
        return MessageBuilder.buildInstructionAcknowledgementText().equals(text == null ? "" : text.trim());
    }

    private String buildProfilePaneMessage(ChatTranscriptEntry entry) {
        if (entry == null) {
            return TextUtils.getText("ai_chat_profile_label");
        }
        if (entry instanceof AssistantProfileTranscriptEntry) {
            AssistantProfileTranscriptEntry assistantProfileEntry = (AssistantProfileTranscriptEntry) entry;
            String profileName = assistantProfileEntry.getProfileName();
            if (profileName != null && !profileName.trim().isEmpty()) {
                return TextUtils.format("ai_chat_profile_message", profileName.trim());
            }
        }
        return TextUtils.getText("ai_chat_profile_label");
    }
}
