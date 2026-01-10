package org.freeplane.plugin.ai.chat;

class AIModelSelection {
    static final String SELECTION_SEPARATOR = "|";
    private final String providerName;
    private final String modelName;

    private AIModelSelection(String providerName, String modelName) {
        this.providerName = providerName;
        this.modelName = modelName;
    }

    static AIModelSelection fromSelectionValue(String selectionValue) {
        if (selectionValue == null || selectionValue.isEmpty()) {
            return null;
        }
        int separatorIndex = selectionValue.indexOf(SELECTION_SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex >= selectionValue.length() - 1) {
            return null;
        }
        String providerName = selectionValue.substring(0, separatorIndex);
        String modelName = selectionValue.substring(separatorIndex + 1);
        if (providerName.isEmpty() || modelName.isEmpty()) {
            return null;
        }
        return new AIModelSelection(providerName, modelName);
    }

    static String createSelectionValue(String providerName, String modelName) {
        return providerName + SELECTION_SEPARATOR + modelName;
    }

    String getProviderName() {
        return providerName;
    }

    String getModelName() {
        return modelName;
    }
}
