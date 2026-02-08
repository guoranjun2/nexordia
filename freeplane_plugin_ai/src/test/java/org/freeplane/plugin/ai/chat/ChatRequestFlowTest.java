package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.Test;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummary;
import org.freeplane.plugin.ai.tools.utilities.ToolCaller;

public class ChatRequestFlowTest {

    @Test
    public void contextTooLargeRetryEvictsAndCompletesAfterSuccessfulRetry() throws Exception {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ChatRequestFlow uut = new ChatRequestFlow(callbacks, new ChatTokenUsageTracker(totals -> {}), 2);
        AIChatService chatService = mock(AIChatService.class);
        when(chatService.chat("question"))
            .thenThrow(new RuntimeException("context too large"))
            .thenReturn("ok");

        uut.beginRequest("question");
        uut.submitRequest(chatService);

        assertThat(callbacks.awaitFinished()).isTrue();
        assertThat(callbacks.assistantResponseCount).isEqualTo(1);
        assertThat(callbacks.assistantErrorCount).isZero();
        assertThat(callbacks.evictOldestTurnCount).isEqualTo(1);
        assertThat(callbacks.synchronizeTranscriptCount).isEqualTo(1);
        assertThat(callbacks.rebuildHistoryCount).isEqualTo(1);
        assertThat(callbacks.restoreCount).isZero();
    }

    @Test
    public void contextTooLargeAfterMaxRetriesRestoresPendingRequest() throws Exception {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ChatRequestFlow uut = new ChatRequestFlow(callbacks, new ChatTokenUsageTracker(totals -> {}), 1);
        AIChatService chatService = mock(AIChatService.class);
        when(chatService.chat("question"))
            .thenThrow(new RuntimeException("context too large"))
            .thenThrow(new RuntimeException("context too large"));

        uut.beginRequest("question");
        uut.submitRequest(chatService);

        assertThat(callbacks.awaitFinished()).isTrue();
        assertThat(callbacks.assistantResponseCount).isZero();
        assertThat(callbacks.assistantErrorCount).isEqualTo(1);
        assertThat(callbacks.evictOldestTurnCount).isEqualTo(1);
        assertThat(callbacks.restoreCount).isEqualTo(1);
    }

    @Test
    public void onProviderUsageRecordsUsageAndRefreshesCounters() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ChatTokenUsageTracker tokenUsageTracker = spy(new ChatTokenUsageTracker(totals -> {}));
        ChatRequestFlow uut = new ChatRequestFlow(callbacks, tokenUsageTracker, 1);
        AssistantProfileChatMemory memory = mock(AssistantProfileChatMemory.class);
        TokenUsage usage = mock(TokenUsage.class);
        when(usage.inputTokenCount()).thenReturn(120);
        when(usage.outputTokenCount()).thenReturn(80);
        when(memory.onResponseTokenUsage(usage)).thenReturn(false);

        uut.updateChatMemory(memory);
        uut.onProviderUsage(usage);

        verify(tokenUsageTracker, times(1)).recordProviderUsage(usage);
        verify(memory, times(1)).onResponseTokenUsage(usage);
        assertThat(callbacks.postResponseEvictionCount).isZero();
        assertThat(callbacks.refreshTokenCountersCount).isZero();
    }

    @Test
    public void onProviderUsageTriggersPostResponseEvictionWhenWindowAdvances() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ChatTokenUsageTracker tokenUsageTracker = spy(new ChatTokenUsageTracker(totals -> {}));
        ChatRequestFlow uut = new ChatRequestFlow(callbacks, tokenUsageTracker, 1);
        AssistantProfileChatMemory memory = mock(AssistantProfileChatMemory.class);
        TokenUsage usage = mock(TokenUsage.class);
        when(usage.inputTokenCount()).thenReturn(120);
        when(usage.outputTokenCount()).thenReturn(80);
        when(memory.onResponseTokenUsage(usage)).thenReturn(true);

        uut.updateChatMemory(memory);
        uut.onProviderUsage(usage);

        verify(tokenUsageTracker, times(1)).recordProviderUsage(usage);
        verify(memory, times(1)).onResponseTokenUsage(usage);
        assertThat(callbacks.postResponseEvictionCount).isEqualTo(1);
        assertThat(callbacks.refreshTokenCountersCount).isZero();
    }

    @Test
    public void onToolCallSummaryStoresSummaryAndAppendsRenderEntry() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ChatRequestFlow uut = new ChatRequestFlow(callbacks, new ChatTokenUsageTracker(totals -> {}), 1);
        AssistantProfileChatMemory memory = AssistantProfileChatMemory.withMaxTokens(500);

        uut.updateChatMemory(memory);
        uut.onToolCallSummary(new ToolCallSummary("searchNodes", "mcp summary", false, ToolCaller.MCP));

        assertThat(callbacks.toolSummaryAppendCount).isEqualTo(1);
        assertThat(callbacks.lastSummaryEntry).isNotNull();
        assertThat(callbacks.lastSummaryEntry.isToolSummary()).isTrue();
        assertThat(callbacks.lastSummaryEntry.toolSummaryText()).isEqualTo("mcp summary");
        assertThat(callbacks.lastSummaryEntry.toolCaller()).isEqualTo(ToolCaller.MCP);
        assertThat(memory.activeConversationRenderEntries())
            .filteredOn(ChatMemoryRenderEntry::isToolSummary)
            .extracting(ChatMemoryRenderEntry::toolSummaryText)
            .contains("mcp summary");
    }

    @Test
    public void onToolCallSummaryDoesNothingWhenToolHistoryHidden() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        callbacks.toolCallHistoryVisible = false;
        ChatRequestFlow uut = new ChatRequestFlow(callbacks, new ChatTokenUsageTracker(totals -> {}), 1);
        AssistantProfileChatMemory memory = AssistantProfileChatMemory.withMaxTokens(500);

        uut.updateChatMemory(memory);
        uut.onToolCallSummary(new ToolCallSummary("searchNodes", "hidden summary", false, ToolCaller.MCP));

        assertThat(callbacks.toolSummaryAppendCount).isZero();
        assertThat(memory.activeConversationRenderEntries())
            .filteredOn(ChatMemoryRenderEntry::isToolSummary)
            .isEmpty();
    }

    private static class RecordingCallbacks implements ChatRequestFlow.RequestCallbacks {

        private final CountDownLatch finishedLatch = new CountDownLatch(1);
        private int assistantResponseCount;
        private int assistantErrorCount;
        private int evictOldestTurnCount;
        private int synchronizeTranscriptCount;
        private int rebuildHistoryCount;
        private int restoreCount;
        private int postResponseEvictionCount;
        private int refreshTokenCountersCount;
        private boolean toolCallHistoryVisible = true;
        private int toolSummaryAppendCount;
        private ChatMemoryRenderEntry lastSummaryEntry;

        @Override
        public void onRequestStarted() {
        }

        @Override
        public void onRequestFinished() {
            finishedLatch.countDown();
        }

        @Override
        public void onRequestRestored(String pendingUserMessage) {
            restoreCount++;
        }

        @Override
        public void onAssistantResponse(String text) {
            assistantResponseCount++;
        }

        @Override
        public void onAssistantError(String text) {
            assistantErrorCount++;
        }

        @Override
        public int snapshotMemorySize() {
            return 3;
        }

        @Override
        public void truncateMemoryToSize(int size) {
        }

        @Override
        public void synchronizeTranscriptWithMemory() {
            synchronizeTranscriptCount++;
        }

        @Override
        public void rebuildHistoryFromTranscript() {
            rebuildHistoryCount++;
        }

        @Override
        public boolean evictOldestTurn() {
            evictOldestTurnCount++;
            return true;
        }

        @Override
        public void onPostResponseEviction() {
            postResponseEvictionCount++;
        }

        @Override
        public void refreshTokenCounters() {
            refreshTokenCountersCount++;
        }

        @Override
        public boolean isToolCallHistoryVisible() {
            return toolCallHistoryVisible;
        }

        @Override
        public void onToolSummaryAppended(ChatMemoryRenderEntry entry) {
            toolSummaryAppendCount++;
            lastSummaryEntry = entry;
        }

        boolean awaitFinished() throws InterruptedException {
            return finishedLatch.await(3, TimeUnit.SECONDS);
        }
    }
}
