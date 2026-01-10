package org.freeplane.plugin.ai.chat;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

class AIModelSelectionController {
    private final AIProviderConfiguration configuration;
    private final AIModelCatalog modelCatalog;
    private final JComboBox<AIModelDescriptor> modelSelectionComboBox;
    private boolean isModelSelectionUpdateInProgress;
    private boolean isModelListLoadInProgress;
    private Consumer<AIModelDescriptor> modelSelectionChangeListener;

    AIModelSelectionController(AIProviderConfiguration configuration, AIModelCatalog modelCatalog) {
        this.configuration = configuration;
        this.modelCatalog = modelCatalog;
        this.modelSelectionComboBox = new JComboBox<>();
        this.modelSelectionComboBox.setRenderer(new ModelSelectionRenderer());
        this.modelSelectionComboBox.addActionListener(event -> onModelSelectionChanged());
    }

    JComboBox<AIModelDescriptor> getModelSelectionComboBox() {
        return modelSelectionComboBox;
    }

    void setModelSelectionChangeListener(Consumer<AIModelDescriptor> modelSelectionChangeListener) {
        this.modelSelectionChangeListener = modelSelectionChangeListener;
    }

    void loadInitialModelSelectionList() {
        updateModelSelectionList(true);
    }

    private void onModelSelectionChanged() {
        if (isModelSelectionUpdateInProgress) {
            return;
        }
        Object selectedValue = modelSelectionComboBox.getSelectedItem();
        if (!(selectedValue instanceof AIModelDescriptor)) {
            configuration.setSelectedModelValue("");
            notifyModelSelectionChange(null);
            return;
        }
        AIModelDescriptor selectedModel = (AIModelDescriptor) selectedValue;
        configuration.setSelectedModelValue(selectedModel.getSelectionValue());
        notifyModelSelectionChange(selectedModel);
    }

    private void updateModelSelectionList(boolean allowsRefresh) {
        if (isModelListLoadInProgress) {
            return;
        }
        isModelListLoadInProgress = true;
        modelSelectionComboBox.setEnabled(false);
        new SwingWorker<List<AIModelDescriptor>, Void>() {
            @Override
            protected List<AIModelDescriptor> doInBackground() {
                return modelCatalog.getAvailableModels(allowsRefresh);
            }

            @Override
            protected void done() {
                List<AIModelDescriptor> modelDescriptors;
                try {
                    modelDescriptors = get();
                } catch (Exception exception) {
                    modelDescriptors = Collections.emptyList();
                }
                applyModelSelectionList(modelDescriptors);
                isModelListLoadInProgress = false;
            }
        }.execute();
    }

    private void applyModelSelectionList(List<AIModelDescriptor> modelDescriptors) {
        isModelSelectionUpdateInProgress = true;
        try {
            DefaultComboBoxModel<AIModelDescriptor> comboBoxModel = new DefaultComboBoxModel<>(
                modelDescriptors.toArray(new AIModelDescriptor[0])
            );
            modelSelectionComboBox.setModel(comboBoxModel);
            modelSelectionComboBox.setSelectedIndex(-1);
            applySelectionFromConfiguration(modelDescriptors);
            modelSelectionComboBox.setEnabled(hasAnyProviderEnabled());
        } finally {
            isModelSelectionUpdateInProgress = false;
        }
    }

    private void applySelectionFromConfiguration(List<AIModelDescriptor> modelDescriptors) {
        String storedSelectionValue = configuration.getStoredSelectedModelValue();
        String selectionValue = configuration.getSelectedModelValue();
        AIModelSelection selection = AIModelSelection.fromSelectionValue(selectionValue);
        if (selection == null) {
            return;
        }
        for (AIModelDescriptor modelDescriptor : modelDescriptors) {
            if (selection.getProviderName().equalsIgnoreCase(modelDescriptor.getProviderName())
                && selection.getModelName().equals(modelDescriptor.getModelName())) {
                modelSelectionComboBox.setSelectedItem(modelDescriptor);
                if (storedSelectionValue == null || storedSelectionValue.isEmpty()) {
                    configuration.setSelectedModelValue(modelDescriptor.getSelectionValue());
                }
                notifyModelSelectionChange(modelDescriptor);
                return;
            }
        }
        configuration.setSelectedModelValue("");
        modelSelectionComboBox.setSelectedIndex(-1);
        notifyModelSelectionChange(null);
    }

    private boolean hasAnyProviderEnabled() {
        boolean hasOpenrouterKey = configuration.getOpenRouterKey() != null && !configuration.getOpenRouterKey().isEmpty();
        boolean hasGeminiKey = configuration.getGeminiKey() != null && !configuration.getGeminiKey().isEmpty();
        return hasOpenrouterKey || hasGeminiKey || configuration.isOllamaEnabled();
    }

    private void notifyModelSelectionChange(AIModelDescriptor modelDescriptor) {
        if (modelSelectionChangeListener != null) {
            modelSelectionChangeListener.accept(modelDescriptor);
        }
    }

    private static class ModelSelectionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof AIModelDescriptor) {
                setText(((AIModelDescriptor) value).getDisplayName());
            }
            return component;
        }
    }
}
