package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ChatRequestFlowTest {

    @Test
    public void contextTooLargeRetryEvictsAndCompletesAfterSuccessfulRetry() throws Exception {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ChatRequestFlow uut = new ChatRequestFlow(callbacks, 2);
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
        assertThat(callbacks.completeDeferredCapacityChecksCount).isEqualTo(1);
        assertThat(callbacks.restoreCount).isZero();
    }

    @Test
    public void contextTooLargeAfterMaxRetriesRestoresPendingRequest() throws Exception {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ChatRequestFlow uut = new ChatRequestFlow(callbacks, 1);
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
        assertThat(callbacks.cancelDeferredCapacityChecksCount).isEqualTo(1);
    }

    private static class RecordingCallbacks implements ChatRequestFlow.RequestCallbacks {

        private final CountDownLatch finishedLatch = new CountDownLatch(1);
        private int assistantResponseCount;
        private int assistantErrorCount;
        private int evictOldestTurnCount;
        private int synchronizeTranscriptCount;
        private int rebuildHistoryCount;
        private int completeDeferredCapacityChecksCount;
        private int cancelDeferredCapacityChecksCount;
        private int restoreCount;

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
        public void deferCapacityChecks() {
        }

        @Override
        public void completeDeferredCapacityChecks() {
            completeDeferredCapacityChecksCount++;
        }

        @Override
        public void cancelDeferredCapacityChecks() {
            cancelDeferredCapacityChecksCount++;
        }

        boolean awaitFinished() throws InterruptedException {
            return finishedLatch.await(3, TimeUnit.SECONDS);
        }
    }
}
