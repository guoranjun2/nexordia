package org.freeplane.plugin.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.freeplane.plugin.ai.chat.AIChatService;
import org.junit.Test;

public class HiddenPromptRequestRunnerTest {

    @Test
    public void submit_reportsFailureAndClearsActiveFlag() throws Exception {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        HiddenPromptRequestRunner uut = new HiddenPromptRequestRunner(callbacks);
        AIChatService chatService = mock(AIChatService.class);
        when(chatService.chat("prompt"))
            .thenThrow(new RuntimeException("provider failure"));

        uut.submit("Rewrite", chatService, "prompt");

        assertThat(callbacks.awaitFinished()).isTrue();
        assertThat(callbacks.startedPromptName).isEqualTo("Rewrite");
        assertThat(callbacks.failedPromptName).isEqualTo("Rewrite");
        assertThat(callbacks.failureMessage).isEqualTo("provider failure");
        assertThat(uut.isRequestActive()).isFalse();
    }

    @Test
    public void cancelActiveRequest_suppressesFailureAndClearsActiveFlag() throws Exception {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        HiddenPromptRequestRunner uut = new HiddenPromptRequestRunner(callbacks);
        AIChatService chatService = mock(AIChatService.class);
        when(chatService.chat("prompt")).thenAnswer(invocation -> {
            callbacks.chatStartedLatch.countDown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return "cancelled";
        });

        uut.submit("Rewrite", chatService, "prompt");
        assertThat(callbacks.awaitChatStarted()).isTrue();

        uut.cancelActiveRequest();

        assertThat(callbacks.awaitFinished()).isTrue();
        assertThat(callbacks.failedPromptName).isNull();
        assertThat(uut.isRequestActive()).isFalse();
        assertThat(uut.cancellationSupplier().get()).isTrue();
    }

    private static class RecordingCallbacks implements HiddenPromptRequestRunner.Callbacks {
        private final CountDownLatch chatStartedLatch = new CountDownLatch(1);
        private final CountDownLatch finishedLatch = new CountDownLatch(1);
        private String startedPromptName;
        private String finishedPromptName;
        private String failedPromptName;
        private String failureMessage;

        @Override
        public void onRequestStarted(String promptName) {
            startedPromptName = promptName;
        }

        @Override
        public void onRequestFinished(String promptName) {
            finishedPromptName = promptName;
            finishedLatch.countDown();
        }

        @Override
        public void onRequestFailed(String promptName, String errorMessage) {
            failedPromptName = promptName;
            failureMessage = errorMessage;
        }

        boolean awaitChatStarted() throws InterruptedException {
            return chatStartedLatch.await(5, TimeUnit.SECONDS);
        }

        boolean awaitFinished() throws InterruptedException {
            return finishedLatch.await(5, TimeUnit.SECONDS);
        }
    }
}
