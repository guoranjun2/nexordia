package org.freeplane.plugin.ai.chat;

interface SingleTurnChatMemory {

    int snapshotSize();

    void truncateTo(int size);

    void deferCapacityChecks();

    void completeDeferredCapacityChecks();

    void cancelDeferredCapacityChecks();

    boolean evictOldestTurn();
}
