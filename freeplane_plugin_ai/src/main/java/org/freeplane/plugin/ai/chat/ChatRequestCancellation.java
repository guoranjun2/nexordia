package org.freeplane.plugin.ai.chat;

public class ChatRequestCancellation {
    private volatile boolean cancelled;

    public void reset() {
        cancelled = false;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
