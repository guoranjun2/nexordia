package org.freeplane.plugin.ai.chat;

import java.util.ArrayList;
import java.util.List;

class ChatTokenUsageState {
    private final List<Long> outputByTurn;
    private final List<Long> inputByTurn;
    private final int currentTurnCount;

    ChatTokenUsageState(List<Long> outputByTurn, List<Long> inputByTurn,
                        int currentTurnCount) {
        this.outputByTurn = copyList(outputByTurn);
        this.inputByTurn = copyList(inputByTurn);
        this.currentTurnCount = currentTurnCount;
    }

    List<Long> getOutputByTurn() {
        return new ArrayList<>(outputByTurn);
    }

    List<Long> getInputByTurn() {
        return new ArrayList<>(inputByTurn);
    }

    int getCurrentTurnCount() {
        return currentTurnCount;
    }

    private static List<Long> copyList(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }
}
