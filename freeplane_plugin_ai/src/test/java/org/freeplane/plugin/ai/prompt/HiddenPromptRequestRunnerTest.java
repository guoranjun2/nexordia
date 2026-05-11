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

    private static class RecordingCallbacks implements HiddenPromptRequestRunner.Callbacks {
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

        boolean awaitFinished() throws InterruptedException {
            return finishedLatch.await(5, TimeUnit.SECONDS);
        }
    }
}
