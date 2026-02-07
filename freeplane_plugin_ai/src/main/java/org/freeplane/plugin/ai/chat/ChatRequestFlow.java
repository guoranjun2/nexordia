package org.freeplane.plugin.ai.chat;

import java.util.function.Supplier;
import javax.swing.SwingWorker;

class ChatRequestFlow {

    interface RequestCallbacks {
        void onRequestStarted();
        void onRequestFinished();
        void onRequestRestored(String pendingUserMessage);
        void onAssistantResponse(String text);
        void onAssistantError(String text);
        int snapshotMemorySize();
        void truncateMemoryToSize(int size);
        void synchronizeTranscriptWithMemory();
        void rebuildHistoryFromTranscript();
        boolean evictOldestTurn();
        void deferCapacityChecks();
        void completeDeferredCapacityChecks();
        void cancelDeferredCapacityChecks();
    }

    private final RequestCallbacks callbacks;
    private final ChatRequestCancellation requestCancellation;
    private final int contextTooLargeMaxRetries;
    private SwingWorker<String, Void> activeWorker;
    private boolean requestInProgress;
    private int activeRequestId;
    private int pendingMemorySize;
    private String pendingUserMessage;
    private int pendingContextTooLargeRetryCount;

    ChatRequestFlow(RequestCallbacks callbacks, int contextTooLargeMaxRetries) {
        this.callbacks = callbacks;
        this.contextTooLargeMaxRetries = contextTooLargeMaxRetries;
        this.requestCancellation = new ChatRequestCancellation();
    }

    boolean isRequestActive() {
        return requestInProgress;
    }

    Supplier<Boolean> cancellationSupplier() {
        return requestCancellation::isCancelled;
    }

    void beginRequest(String userMessage) {
        requestCancellation.reset();
        activeRequestId++;
        pendingUserMessage = userMessage;
        pendingMemorySize = callbacks.snapshotMemorySize();
        pendingContextTooLargeRetryCount = 0;
        requestInProgress = true;
        callbacks.deferCapacityChecks();
        callbacks.onRequestStarted();
    }

    void submitRequest(AIChatService chatService) {
        executeRequestWorker(chatService, pendingUserMessage, activeRequestId);
    }

    void refreshPendingMemorySnapshot() {
        pendingMemorySize = callbacks.snapshotMemorySize();
    }

    void cancelActiveRequest() {
        if (!isRequestActive()) {
            return;
        }
        requestCancellation.cancel();
        activeRequestId++;
        if (activeWorker != null) {
            activeWorker.cancel(true);
        }
        requestInProgress = false;
        restorePendingRequest();
    }

    void restorePendingRequest() {
        callbacks.truncateMemoryToSize(pendingMemorySize);
        callbacks.cancelDeferredCapacityChecks();
        callbacks.synchronizeTranscriptWithMemory();
        callbacks.rebuildHistoryFromTranscript();
        activeWorker = null;
        requestInProgress = false;
        callbacks.onRequestRestored(pendingUserMessage);
        callbacks.onRequestFinished();
        clearPendingRequestState();
    }

    void resetPendingState() {
        clearPendingRequestState();
    }

    private void executeRequestWorker(AIChatService chatService, String userMessage, int requestId) {
        activeWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return chatService.chat(userMessage);
            }

            @Override
            protected void done() {
                if (requestId != activeRequestId || requestCancellation.isCancelled()) {
                    return;
                }
                try {
                    callbacks.onAssistantResponse(get());
                    finishRequest();
                } catch (Exception error) {
                    if (retryAfterContextTooLarge(error, chatService, requestId)) {
                        return;
                    }
                    callbacks.onAssistantError(String.valueOf(error.getMessage()));
                    restorePendingRequest();
                }
            }
        };
        activeWorker.execute();
    }

    private boolean retryAfterContextTooLarge(Exception error, AIChatService chatService, int requestId) {
        if (!isContextTooLargeError(error)) {
            return false;
        }
        if (pendingContextTooLargeRetryCount >= contextTooLargeMaxRetries) {
            return false;
        }
        callbacks.truncateMemoryToSize(pendingMemorySize);
        if (!callbacks.evictOldestTurn()) {
            return false;
        }
        pendingContextTooLargeRetryCount++;
        callbacks.synchronizeTranscriptWithMemory();
        callbacks.rebuildHistoryFromTranscript();
        executeRequestWorker(chatService, pendingUserMessage, requestId);
        return true;
    }

    private boolean isContextTooLargeError(Exception error) {
        if (error == null || error.getMessage() == null) {
            return false;
        }
        String message = error.getMessage().toLowerCase();
        return message.contains("context") && (message.contains("too large")
            || message.contains("length") || message.contains("maximum context"));
    }

    private void finishRequest() {
        callbacks.completeDeferredCapacityChecks();
        activeWorker = null;
        requestInProgress = false;
        callbacks.onRequestFinished();
        clearPendingRequestState();
    }

    private void clearPendingRequestState() {
        pendingMemorySize = 0;
        pendingUserMessage = null;
        pendingContextTooLargeRetryCount = 0;
    }
}
