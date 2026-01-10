package org.freeplane.plugin.ai.chat;

import java.util.Objects;

class AIModelDescriptor {
    private final String providerName;
    private final String modelName;
    private final String displayName;
    private final boolean isFreeModel;

    AIModelDescriptor(String providerName, String modelName, String displayName, boolean isFreeModel) {
        this.providerName = Objects.requireNonNull(providerName, "providerName");
        this.modelName = Objects.requireNonNull(modelName, "modelName");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.isFreeModel = isFreeModel;
    }

    String getProviderName() {
        return providerName;
    }

    String getModelName() {
        return modelName;
    }

    String getDisplayName() {
        return displayName;
    }

    boolean isFreeModel() {
        return isFreeModel;
    }

    String getSelectionValue() {
        return AIModelSelection.createSelectionValue(providerName, modelName);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
