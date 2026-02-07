package org.freeplane.plugin.ai.chat;

interface SingleTurnChatMemory {

    int snapshotSize();

    void truncateTo(int size);

    boolean evictOldestTurn();
}
