package org.freeplane.plugin.ai.chat;

import dev.langchain4j.memory.ChatMemory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptId;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRecord;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptStore;
import org.freeplane.plugin.ai.chat.history.MapRootShortTextCount;
import org.freeplane.plugin.ai.maps.AvailableMaps;

public class LiveChatController {

    public interface SessionActivationHandler {
        void activate(ChatMemory chatMemory, boolean fromTranscriptRestore);
    }

    private final AIChatPanel owner;
    private final LiveChatSessionManager liveChatSessionManager;
    private final DateTimeFormatter chatNameFormatter;
    private final SessionActivationHandler sessionActivationHandler;
    private final ChatTranscriptStore transcriptStore;
    private final TranscriptMemoryMapper transcriptMemoryMapper;
    private final ChatMemorySettings chatMemorySettings;
    private final MapRootShortTextFormatter mapRootShortTextFormatter;
    private final MapRootShortTextCountsMerger mapRootShortTextCountsMerger;
    private final Supplier<ChatTokenUsageState> tokenUsageStateSupplier;
    private static final String TRANSCRIPT_HIDDEN_SYSTEM_MESSAGE =
        "System message: The messages in this session include a restored transcript of a prior chat. "
            + "Treat those messages as the earlier conversation context, not as hallucinations. "
            + "The currently opened map may differ from the maps discussed in that transcript. "
            + "Confirm the map context with the user when needed. The real conversation begins after this message. ";

    public LiveChatController(AIChatPanel parent,
                              AvailableMaps availableMaps,
                              TextController textController,
                              DateTimeFormatter chatNameFormatter,
                              SessionActivationHandler sessionActivationHandler,
                              Supplier<ChatTokenUsageState> tokenUsageStateSupplier) {
        this.owner = parent;
        this.chatNameFormatter = chatNameFormatter;
        this.sessionActivationHandler = sessionActivationHandler;
        this.tokenUsageStateSupplier = tokenUsageStateSupplier;
        this.liveChatSessionManager = new LiveChatSessionManager();
        this.transcriptStore = new ChatTranscriptStore();
        this.transcriptMemoryMapper = new TranscriptMemoryMapper();
        this.chatMemorySettings = new ChatMemorySettings();
        this.mapRootShortTextFormatter = new MapRootShortTextFormatter(availableMaps, textController);
        this.mapRootShortTextCountsMerger = new MapRootShortTextCountsMerger();
    }

    public void initialize(ChatMemory chatMemory) {
        LiveChatSession initialSession = liveChatSessionManager.createSession(chatMemory, buildDefaultChatName());
        liveChatSessionManager.setCurrentSession(initialSession.getId());
        sessionActivationHandler.activate(chatMemory, false);
    }

    public void startNewChat() {
        switchToNewSession();
    }

    public void openLiveChats() {
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
        synchronizeTranscriptWithMemory();
    }

    public void recordAssistantMessage(String message) {
        synchronizeTranscriptWithMemory();
    }

    public void recordAssistantProfileMessage(AssistantProfileSwitchMessage message) {
        synchronizeTranscriptWithMemory();
    }

    public List<ChatTranscriptEntry> snapshotTranscriptEntries() {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(session.getTranscriptEntries());
    }

    public void persistCurrentSessionIfNeeded() {
        persistCurrentSession();
    }

    public ChatTokenUsageState getCurrentTokenUsageState() {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return null;
        }
        return session.getTokenUsageState();
    }

    public boolean canUndo() {
        AssistantProfileChatMemory memory = activeAssistantProfileChatMemory();
        return memory != null && memory.canUndo();
    }

    public boolean canRedo() {
        AssistantProfileChatMemory memory = activeAssistantProfileChatMemory();
        return memory != null && memory.canRedo();
    }

    public String undoLastTurn() {
        AssistantProfileChatMemory memory = activeAssistantProfileChatMemory();
        if (memory == null || !memory.canUndo()) {
            return "";
        }
        String userMessage = memory.undo();
        synchronizeTranscriptWithMemory();
        return userMessage;
    }

    public void redoLastTurn() {
        AssistantProfileChatMemory memory = activeAssistantProfileChatMemory();
        if (memory == null || !memory.canRedo()) {
            return;
        }
        memory.redo();
        synchronizeTranscriptWithMemory();
    }

    public void synchronizeTranscriptWithMemory() {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        session.setTranscriptEntries(transcriptMemoryMapper.toTranscriptEntries(session.getChatMemory()));
        session.setLastActivityTimestamp(System.currentTimeMillis());
    }

    private void switchToNewSession() {
        persistCurrentSession();
        ChatMemory newChatMemory = createChatMemory();
        LiveChatSession newSession = liveChatSessionManager.createSession(newChatMemory, buildDefaultChatName());
        switchToSession(newSession.getId(), false, false);
    }

    private void switchToSession(LiveChatSessionId sessionId) {
        switchToSession(sessionId, true, false);
    }

    private void switchToSession(LiveChatSessionId sessionId, boolean saveCurrent) {
        switchToSession(sessionId, saveCurrent, false);
    }

    private void switchToSession(LiveChatSessionId sessionId, boolean saveCurrent, boolean fromTranscriptRestore) {
        if (sessionId == null) {
            return;
        }
        if (saveCurrent) {
            persistCurrentSession();
        }
        liveChatSessionManager.setCurrentSession(sessionId);
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        sessionActivationHandler.activate(session.getChatMemory(), fromTranscriptRestore);
    }

    private void closeSession(LiveChatSessionId sessionId) {
        if (sessionId == null) {
            return;
        }
        LiveChatSession activeSession = liveChatSessionManager.getCurrentSession();
        if (activeSession != null && sessionId.equals(activeSession.getId())) {
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
        ChatMemory newChatMemory = createChatMemory();
        LiveChatSession newSession = liveChatSessionManager.createSession(newChatMemory, buildDefaultChatName());
        liveChatSessionManager.setCurrentSession(newSession.getId());
        sessionActivationHandler.activate(newChatMemory, false);
    }

    private void persistCurrentSession() {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        if (session == null) {
            return;
        }
        session.setTranscriptEntries(transcriptMemoryMapper.toTranscriptEntries(session.getChatMemory()));
        storeTokenUsageState(session);
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

    private void startChatFromTranscriptInternal(ChatTranscriptId transcriptId) {
        if (transcriptId == null) {
            return;
        }
        persistCurrentSession();
        ChatTranscriptRecord record = transcriptStore.load(transcriptId);
        if (record == null) {
            return;
        }
        ChatMemory newChatMemory = createChatMemory();
        LiveChatSession newSession = liveChatSessionManager.createSession(newChatMemory,
            record.getDisplayName() == null || record.getDisplayName().trim().isEmpty()
                ? buildDefaultChatName()
                : record.getDisplayName());
        newSession.setTranscriptId(transcriptId);
        newSession.setLastActivityTimestamp(record.getTimestamp());
        newSession.setMapRootShortTextCounts(record.getMapRootShortTextCounts());
        newSession.setTranscriptEntries(record.getEntries() == null
            ? new ArrayList<>()
            : new ArrayList<>(record.getEntries()));
        seedTranscriptMemory(newSession, record);
        switchToSession(newSession.getId(), false, true);
    }

    private void seedTranscriptMemory(LiveChatSession session, ChatTranscriptRecord record) {
        if (session == null || record == null) {
            return;
        }
        transcriptMemoryMapper.seedTranscriptWithHiddenExchange(session.getChatMemory(), record.getEntries(),
            TRANSCRIPT_HIDDEN_SYSTEM_MESSAGE);
        AssistantProfileChatMemory memory = activeAssistantProfileChatMemory(session);
        if (memory != null) {
            memory.initializeUndoRedoFromMessages();
            memory.expandWindowAfterTranscriptRestoreIfUnderutilized();
        }
    }

    private AssistantProfileChatMemory activeAssistantProfileChatMemory() {
        LiveChatSession session = liveChatSessionManager.getCurrentSession();
        return activeAssistantProfileChatMemory(session);
    }

    private AssistantProfileChatMemory activeAssistantProfileChatMemory(LiveChatSession session) {
        if (session == null) {
            return null;
        }
        ChatMemory memory = session.getChatMemory();
        if (memory instanceof AssistantProfileChatMemory) {
            return (AssistantProfileChatMemory) memory;
        }
        return null;
    }

    private ChatMemory createChatMemory() {
        return AssistantProfileChatMemory.withMaxTokens(chatMemorySettings.getMaximumTokenCount());
    }

    private void storeTokenUsageState(LiveChatSession session) {
        if (session == null || tokenUsageStateSupplier == null) {
            return;
        }
        session.setTokenUsageState(tokenUsageStateSupplier.get());
    }
}
