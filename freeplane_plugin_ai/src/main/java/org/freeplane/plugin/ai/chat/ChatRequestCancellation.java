package org.freeplane.plugin.ai.chat;

class ChatRequestCancellation {
    private volatile boolean cancelled;

    void reset() {
        cancelled = false;
    }

    void cancel() {
        cancelled = true;
    }

    boolean isCancelled() {
        return cancelled;
    }
}
