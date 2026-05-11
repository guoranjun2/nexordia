package org.freeplane.plugin.ai.prompt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.freeplane.core.util.TextUtils;

class AiPromptManagerDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_NEW_PROMPT_NAME = "New Prompt";

    private final AiPromptActionRegistry promptActionRegistry;
    private final EditorState editorState;
    private final DefaultListModel<AiPrompt> listModel = new DefaultListModel<AiPrompt>();
    private final JList<AiPrompt> promptsList = new JList<AiPrompt>(listModel);
    private final JTextField nameField = new JTextField();
    private final JTextArea promptArea = new JTextArea();
    private final JCheckBox showInChatCheckBox = new JCheckBox(TextUtils.getText("ai_prompt_show_in_chat"));
    private final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    private final JButton newButton = new JButton(TextUtils.getText("ai_prompt_new"));
    private final JButton sendButton = new JButton(TextUtils.getText("ai_prompt_send"));
    private final JButton saveAsNewButton = new JButton(TextUtils.getText("ai_prompt_save_as_new"));
    private final JButton saveButton = new JButton(TextUtils.getText("save"));
    private final JButton deleteButton = new JButton(TextUtils.getText("delete"));
    private final JButton closeButton = new JButton(TextUtils.getText("close"));
    private boolean updatingFields;
    private boolean updatingSelection;

    AiPromptManagerDialog(Component owner, AiPromptActionRegistry promptActionRegistry) {
        super(findOwnerWindow(owner));
        this.promptActionRegistry = promptActionRegistry;
        this.editorState = promptActionRegistry.getDialogState();
        setTitle(TextUtils.getText("ai_prompt_manager_title"));
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                requestClose();
            }
        });
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(520, 390));
        buildUi();
        refreshUi();
        pack();
        ensureButtonRowIsVisible();
        setLocationRelativeTo(owner);
    }

    void openDialog() {
        setVisible(true);
    }

    private void buildUi() {
        promptsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        promptsList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            handlePromptSelectionChange();
        });

        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateDraftFromFields();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateDraftFromFields();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateDraftFromFields();
            }
        };
        nameField.getDocument().addDocumentListener(documentListener);
        promptArea.getDocument().addDocumentListener(documentListener);
        showInChatCheckBox.addActionListener(event -> updateDraftFromFields());

        JPanel listPanel = new JPanel(new BorderLayout(5, 5));
        listPanel.add(new JLabel(TextUtils.getText("ai_prompt_list_label")), BorderLayout.NORTH);
        listPanel.add(new JScrollPane(promptsList), BorderLayout.CENTER);

        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.add(new JLabel(TextUtils.getText("ai_prompt_name_label")), BorderLayout.NORTH);
        namePanel.add(nameField, BorderLayout.CENTER);

        JPanel promptPanel = new JPanel(new BorderLayout(5, 5));
        promptPanel.add(new JLabel(TextUtils.getText("ai_prompt_prompt_label")), BorderLayout.NORTH);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptPanel.add(new JScrollPane(promptArea), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.add(namePanel, BorderLayout.NORTH);
        rightPanel.add(promptPanel, BorderLayout.CENTER);
        rightPanel.add(showInChatCheckBox, BorderLayout.SOUTH);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(listPanel, BorderLayout.WEST);
        contentPanel.add(rightPanel, BorderLayout.CENTER);

        newButton.addActionListener(event -> beginNewPromptDraft());
        sendButton.addActionListener(event -> sendPrompt());
        saveAsNewButton.addActionListener(event -> savePromptAsNew());
        saveButton.addActionListener(event -> savePrompt());
        deleteButton.addActionListener(event -> deletePrompt());
        closeButton.addActionListener(event -> requestClose());

        buttonPanel.add(newButton);
        buttonPanel.add(sendButton);
        buttonPanel.add(saveAsNewButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void handlePromptSelectionChange() {
        if (updatingSelection) {
            return;
        }
        int selectedIndex = promptsList.getSelectedIndex();
        int currentIndex = editorState.getSelectedSavedPromptIndex();
        if (selectedIndex < 0 || selectedIndex == currentIndex) {
            refreshPromptSelection();
            return;
        }
        updateDraftFromFields();
        if (handleDirtyDecision(() -> editorState.selectSavedPrompt(selectedIndex))) {
            refreshUi();
        }
    }

    private void beginNewPromptDraft() {
        updateDraftFromFields();
        if (handleDirtyDecision(editorState::beginNewDraft)) {
            refreshUi();
            SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        }
    }

    private void deletePrompt() {
        if (!editorState.isEditingSavedPrompt()) {
            return;
        }
        updateDraftFromFields();
        if (handleDirtyDecision(() -> {
            editorState.deleteSelectedPrompt();
            promptActionRegistry.persistStateAndRefreshMenus();
        })) {
            refreshUi();
        }
    }

    private void savePrompt() {
        updateDraftFromFields();
        editorState.save(defaultNewPromptName());
        promptActionRegistry.persistStateAndRefreshMenus();
        refreshUi();
    }

    private void savePromptAsNew() {
        if (!editorState.canSaveAsNew()) {
            return;
        }
        updateDraftFromFields();
        editorState.saveAsNew(defaultNewPromptName());
        promptActionRegistry.persistStateAndRefreshMenus();
        refreshUi();
    }

    private void sendPrompt() {
        updateDraftFromFields();
        promptActionRegistry.runPrompt(editorState.getCurrentDraft());
    }

    private void requestClose() {
        updateDraftFromFields();
        promptActionRegistry.persistStateIfChanged();
        closeDialog();
    }

    private boolean handleDirtyDecision(Runnable continueAction) {
        if (!editorState.isDirty()) {
            continueAction.run();
            return true;
        }
        DirtyDraftDecision decision = askDirtyDraftDecision();
        switch (decision) {
            case SAVE:
                savePrompt();
                continueAction.run();
                return true;
            case DISCARD:
                continueAction.run();
                return true;
            case CANCEL:
            default:
                refreshUi();
                return false;
        }
    }

    private DirtyDraftDecision askDirtyDraftDecision() {
        Object[] options = {
            TextUtils.getText("save"),
            TextUtils.getText("ai_prompt_discard"),
            TextUtils.getText("cancel")
        };
        String message = TextUtils.getText("ai_prompt_unsaved_changes")
            + "\n"
            + TextUtils.getText("ai_prompt_unsaved_changes_explanation");
        int result = JOptionPane.showOptionDialog(
            this,
            message,
            TextUtils.getText("ai_prompt_manager_title"),
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        switch (result) {
            case JOptionPane.YES_OPTION:
                return DirtyDraftDecision.SAVE;
            case JOptionPane.NO_OPTION:
                return DirtyDraftDecision.DISCARD;
            default:
                return DirtyDraftDecision.CANCEL;
        }
    }

    private void updateDraftFromFields() {
        if (updatingFields) {
            return;
        }
        editorState.updateDraft(nameField.getText(), promptArea.getText(), showInChatCheckBox.isSelected());
        refreshButtons();
    }

    private void refreshUi() {
        refreshPromptList();
        refreshEditorFromState();
        refreshButtons();
    }

    private void refreshPromptList() {
        updatingSelection = true;
        try {
            listModel.clear();
            for (AiPrompt prompt : editorState.getSavedPrompts()) {
                listModel.addElement(prompt);
            }
            refreshPromptSelection();
        }
        finally {
            updatingSelection = false;
        }
    }

    private void refreshPromptSelection() {
        int selectedIndex = editorState.getSelectedSavedPromptIndex();
        if (selectedIndex >= 0 && selectedIndex < listModel.getSize()) {
            promptsList.setSelectedIndex(selectedIndex);
        }
        else {
            promptsList.clearSelection();
        }
    }

    private void refreshEditorFromState() {
        updatingFields = true;
        try {
            AiPrompt currentDraft = editorState.getCurrentDraft();
            nameField.setText(currentDraft.getName());
            promptArea.setText(currentDraft.getPrompt() == null ? "" : currentDraft.getPrompt());
            showInChatCheckBox.setSelected(currentDraft.isShowInChat());
        }
        finally {
            updatingFields = false;
        }
    }

    private void refreshButtons() {
        deleteButton.setEnabled(editorState.isEditingSavedPrompt());
        saveAsNewButton.setEnabled(editorState.canSaveAsNew());
    }

    private String defaultNewPromptName() {
        return TextUtils.getText("ai_prompt_new_name");
    }

    private void ensureButtonRowIsVisible() {
        Insets insets = getInsets();
        int minimumWidth = Math.max(
            getWidth(),
            buttonPanel.getPreferredSize().width + insets.left + insets.right);
        setMinimumSize(new Dimension(minimumWidth, getHeight()));
        if (getWidth() < minimumWidth) {
            setSize(minimumWidth, getHeight());
        }
    }

    private void closeDialog() {
        setVisible(false);
        dispose();
    }

    private static Window findOwnerWindow(Component owner) {
        if (owner instanceof Window) {
            return (Window) owner;
        }
        return owner == null ? null : SwingUtilities.getWindowAncestor(owner);
    }

    enum DirtyDraftDecision {
        SAVE,
        DISCARD,
        CANCEL
    }

    static class EditorState {
        private final List<AiPrompt> savedPrompts = new ArrayList<AiPrompt>();
        private String selectedPromptName = "";
        private AiPrompt currentDraft = emptyDraft();

        void loadState(List<AiPrompt> prompts, AiPromptStore.PersistedDialogState dialogState, String defaultName) {
            savedPrompts.clear();
            for (AiPrompt prompt : AiPromptNameValidator.normalizeAndDeduplicate(prompts, defaultName)) {
                savedPrompts.add(prompt.copy());
            }
            selectedPromptName = dialogState == null ? "" : safe(dialogState.getSelectedPromptName());
            currentDraft = dialogState == null ? emptyDraft() : safePrompt(dialogState.getDraft());
            if (!selectedPromptName.isEmpty() && findSavedPromptIndexByName(selectedPromptName) < 0) {
                selectedPromptName = "";
            }
        }

        void loadSavedPrompts(List<AiPrompt> prompts) {
            loadState(prompts, new AiPromptStore.PersistedDialogState(), DEFAULT_NEW_PROMPT_NAME);
        }

        List<AiPrompt> getSavedPrompts() {
            List<AiPrompt> copies = new ArrayList<AiPrompt>();
            for (AiPrompt prompt : savedPrompts) {
                copies.add(prompt.copy());
            }
            return copies;
        }

        void selectSavedPrompt(int index) {
            AiPrompt selectedPrompt = savedPrompts.get(index).copy();
            selectedPromptName = selectedPrompt.getName();
            currentDraft = selectedPrompt.copy();
        }

        void beginNewDraft() {
            selectedPromptName = "";
            currentDraft = emptyDraft();
        }

        boolean isEditingSavedPrompt() {
            return getSelectedSavedPromptIndex() >= 0;
        }

        boolean canSaveAsNew() {
            return isEditingSavedPrompt();
        }

        int getSelectedSavedPromptIndex() {
            return findSavedPromptIndexByName(selectedPromptName);
        }

        AiPrompt getCurrentDraft() {
            return currentDraft.copy();
        }

        void updateDraft(String name, String prompt, boolean showInChat) {
            currentDraft.setName(safe(name));
            currentDraft.setPrompt(safe(prompt));
            currentDraft.setShowInChat(showInChat);
        }

        boolean isDirty() {
            return !samePrompt(currentDraft, baselineDraft());
        }

        void save(String defaultName) {
            AiPrompt normalizedDraft = AiPromptNameValidator.normalizeForSave(
                currentDraft,
                promptsExcludingSelected(),
                defaultName);
            int selectedIndex = getSelectedSavedPromptIndex();
            if (selectedIndex >= 0) {
                savedPrompts.set(selectedIndex, normalizedDraft.copy());
            }
            else {
                savedPrompts.add(normalizedDraft.copy());
            }
            selectedPromptName = normalizedDraft.getName();
            currentDraft = normalizedDraft.copy();
        }

        void saveAsNew(String defaultName) {
            if (!canSaveAsNew()) {
                throw new IllegalStateException("Save as new requires a selected saved prompt.");
            }
            AiPrompt normalizedDraft = AiPromptNameValidator.normalizeForSave(
                currentDraft,
                getSavedPrompts(),
                defaultName);
            savedPrompts.add(normalizedDraft.copy());
            selectedPromptName = normalizedDraft.getName();
            currentDraft = normalizedDraft.copy();
        }

        void deleteSelectedPrompt() {
            int selectedIndex = getSelectedSavedPromptIndex();
            if (selectedIndex < 0) {
                return;
            }
            savedPrompts.remove(selectedIndex);
            if (savedPrompts.isEmpty()) {
                beginNewDraft();
                return;
            }
            selectSavedPrompt(Math.min(selectedIndex, savedPrompts.size() - 1));
        }

        AiPromptStore.PersistedDialogState createPersistedDialogState() {
            return new AiPromptStore.PersistedDialogState(selectedPromptName, currentDraft);
        }

        private List<AiPrompt> promptsExcludingSelected() {
            int selectedIndex = getSelectedSavedPromptIndex();
            List<AiPrompt> prompts = new ArrayList<AiPrompt>();
            for (int index = 0; index < savedPrompts.size(); index++) {
                if (index == selectedIndex) {
                    continue;
                }
                prompts.add(savedPrompts.get(index).copy());
            }
            return prompts;
        }

        private AiPrompt baselineDraft() {
            int selectedIndex = getSelectedSavedPromptIndex();
            return selectedIndex >= 0 ? savedPrompts.get(selectedIndex).copy() : emptyDraft();
        }

        private int findSavedPromptIndexByName(String promptName) {
            for (int index = 0; index < savedPrompts.size(); index++) {
                if (safe(savedPrompts.get(index).getName()).equals(promptName)) {
                    return index;
                }
            }
            return -1;
        }

        private static boolean samePrompt(AiPrompt left, AiPrompt right) {
            return safe(left.getName()).equals(safe(right.getName()))
                && safe(left.getPrompt()).equals(safe(right.getPrompt()))
                && left.isShowInChat() == right.isShowInChat();
        }

        private static AiPrompt safePrompt(AiPrompt prompt) {
            AiPrompt safePrompt = prompt == null ? emptyDraft() : prompt.copy();
            safePrompt.setName(safe(safePrompt.getName()));
            safePrompt.setPrompt(safe(safePrompt.getPrompt()));
            return safePrompt;
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }

        private static AiPrompt emptyDraft() {
            return new AiPrompt("", "", false);
        }
    }
}
