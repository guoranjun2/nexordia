package org.freeplane.plugin.ai.prompt;

import javax.swing.SwingWorker;

import org.freeplane.plugin.ai.chat.AIChatService;

public class HiddenPromptRequestRunner {

    public interface Callbacks {
        void onRequestStarted(String promptName);
        void onRequestFinished(String promptName);
        void onRequestFailed(String promptName, String errorMessage);
    }

    private final Callbacks callbacks;
    private SwingWorker<String, Void> activeWorker;
    private boolean requestActive;

    public HiddenPromptRequestRunner(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public boolean isRequestActive() {
        return requestActive;
    }

    public void submit(String promptName, AIChatService chatService, String userMessage) {
        if (requestActive) {
            throw new IllegalStateException("A hidden prompt request is already active.");
        }
        requestActive = true;
        if (callbacks != null) {
            callbacks.onRequestStarted(promptName);
        }
        activeWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return chatService.chat(userMessage);
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception error) {
                    if (callbacks != null) {
                        callbacks.onRequestFailed(promptName, normalizeErrorMessage(error));
                    }
                } finally {
                    activeWorker = null;
                    requestActive = false;
                    if (callbacks != null) {
                        callbacks.onRequestFinished(promptName);
                    }
                }
            }
        };
        activeWorker.execute();
    }

    private String normalizeErrorMessage(Exception error) {
        Throwable rootCause = rootCause(error);
        String message = rootCause.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return rootCause.getClass().getSimpleName();
        }
        return message;
    }

    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
