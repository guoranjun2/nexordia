package org.freeplane.plugin.ai.prompt.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingWorker;
import org.freeplane.core.util.TextUtils;
import org.freeplane.plugin.ai.model.AIModelCatalog;
import org.freeplane.plugin.ai.model.AIModelDescriptor;
import org.freeplane.plugin.ai.model.AIModelSelection;
import org.freeplane.plugin.ai.model.AIProviderConfiguration;

public class AiPromptModelSelectionController {
    private final AIProviderConfiguration configuration;
    private final AIModelCatalog modelCatalog;
    private final JComboBox<AIModelDescriptor> modelSelectionComboBox;
    private List<AIModelDescriptor> availableModelDescriptors = Collections.emptyList();
    private boolean modelSelectionUpdateInProgress;
    private boolean modelListLoadInProgress;
    private Consumer<String> modelSelectionChangeListener;

    public AiPromptModelSelectionController(AIProviderConfiguration configuration, AIModelCatalog modelCatalog) {
        this.configuration = configuration;
        this.modelCatalog = modelCatalog;
        this.modelSelectionComboBox = new JComboBox<AIModelDescriptor>();
        this.modelSelectionComboBox.setEditable(false);
        this.modelSelectionComboBox.addActionListener(event -> onModelSelectionChanged());
    }

    public JComboBox<AIModelDescriptor> getModelSelectionComboBox() {
        return modelSelectionComboBox;
    }

    public void setModelSelectionChangeListener(Consumer<String> modelSelectionChangeListener) {
        this.modelSelectionChangeListener = modelSelectionChangeListener;
    }

    public void refreshModelSelectionList(String selectionValue) {
        if (modelListLoadInProgress) {
            return;
        }
        modelListLoadInProgress = true;
        modelSelectionComboBox.setEnabled(false);
        new SwingWorker<List<AIModelDescriptor>, Void>() {
            @Override
            protected List<AIModelDescriptor> doInBackground() {
                return modelCatalog.getAvailableModels(true);
            }

            @Override
            protected void done() {
                List<AIModelDescriptor> modelDescriptors;
                try {
                    modelDescriptors = get();
                } catch (Exception exception) {
                    modelDescriptors = Collections.emptyList();
                }
                applyModelSelectionList(modelDescriptors, selectionValue);
                modelListLoadInProgress = false;
            }
        }.execute();
    }

    public void setSelectedModelSelectionValue(String selectionValue) {
        applySelectionToCurrentModelList(selectionValue);
    }

    public String getSelectedModelSelectionValue() {
        Object selectedItem = modelSelectionComboBox.getSelectedItem();
        if (!(selectedItem instanceof AIModelDescriptor)) {
            return "";
        }
        return ((AIModelDescriptor) selectedItem).getSelectionValue();
    }

    void applyModelSelectionList(List<AIModelDescriptor> modelDescriptors, String selectionValue) {
        List<AIModelDescriptor> sortedModelDescriptors = new ArrayList<AIModelDescriptor>(modelDescriptors);
        sortedModelDescriptors.sort(Comparator.comparing(AIModelDescriptor::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        availableModelDescriptors = sortedModelDescriptors;
        applySelectionToCurrentModelList(selectionValue);
    }

    private void applySelectionToCurrentModelList(String selectionValue) {
        modelSelectionUpdateInProgress = true;
        try {
            DefaultComboBoxModel<AIModelDescriptor> comboBoxModel = new DefaultComboBoxModel<AIModelDescriptor>();
            AIModelDescriptor currentModelOption = AIModelDescriptor.useCurrentModelOption(
                TextUtils.getText("ai_prompt_use_current_model"));
            comboBoxModel.addElement(currentModelOption);
            for (AIModelDescriptor modelDescriptor : availableModelDescriptors) {
                comboBoxModel.addElement(modelDescriptor);
            }
            modelSelectionComboBox.setModel(comboBoxModel);
            AIModelDescriptor selectedDescriptor = resolveSelectedDescriptor(selectionValue, comboBoxModel, currentModelOption);
            modelSelectionComboBox.setSelectedItem(selectedDescriptor);
            modelSelectionComboBox.setEnabled(hasAnyProviderEnabled());
        } finally {
            modelSelectionUpdateInProgress = false;
        }
    }

    private AIModelDescriptor resolveSelectedDescriptor(String selectionValue,
                                                        DefaultComboBoxModel<AIModelDescriptor> comboBoxModel,
                                                        AIModelDescriptor currentModelOption) {
        AIModelSelection selection = AIModelSelection.fromSelectionValue(selectionValue);
        if (selection == null) {
            return currentModelOption;
        }
        for (AIModelDescriptor modelDescriptor : availableModelDescriptors) {
            if (selection.getProviderName().equalsIgnoreCase(modelDescriptor.getProviderName())
                && selection.getModelName().equals(modelDescriptor.getModelName())) {
                return modelDescriptor;
            }
        }
        AIModelDescriptor unavailableDescriptor = AIModelDescriptor.unavailable(
            selection.getProviderName(),
            selection.getModelName());
        comboBoxModel.addElement(unavailableDescriptor);
        return unavailableDescriptor;
    }

    private void onModelSelectionChanged() {
        if (modelSelectionUpdateInProgress) {
            return;
        }
        if (modelSelectionChangeListener != null) {
            modelSelectionChangeListener.accept(getSelectedModelSelectionValue());
        }
    }

    private boolean hasAnyProviderEnabled() {
        boolean hasOpenrouterKey = configuration.getOpenRouterKey() != null && !configuration.getOpenRouterKey().isEmpty();
        boolean hasGeminiKey = configuration.getGeminiKey() != null && !configuration.getGeminiKey().isEmpty();
        return hasOpenrouterKey || hasGeminiKey || configuration.hasOllamaServiceAddress();
    }
}
