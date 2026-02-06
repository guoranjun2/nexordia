package org.freeplane.plugin.ai.chat;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

class AssistantProfilePaneBuilder {
    private final AssistantProfileSelectionModel selectionModel;
    private final AssistantProfileSelectionSync selectionSync;
    private final JComboBox<AssistantProfile> selector = new JComboBox<>();
    private final JButton manageProfilesButton;
    private boolean updatingSelection;
    private JPanel panel;

    AssistantProfilePaneBuilder(AssistantProfileSelectionModel selectionModel,
                                AssistantProfileSelectionSync selectionSync,
                                Icon assistantProfileIcon) {
        this.selectionModel = selectionModel;
        this.selectionSync = selectionSync;
        this.manageProfilesButton = new JButton(assistantProfileIcon);
    }

    void initialize() {
        selector.addActionListener(event -> handleAssistantProfileSelection());
        manageProfilesButton.addActionListener(event -> openAssistantProfileManager());
        setAssistantProfileSelection(selectionModel.getSelectedProfile(), true);
    }

    JPanel buildPanel() {
        if (panel == null) {
            panel = new JPanel(new BorderLayout(5, 0));
            panel.add(selector, BorderLayout.CENTER);
            manageProfilesButton.setToolTipText("Manage profiles");
            panel.add(manageProfilesButton, BorderLayout.EAST);
        }
        return panel;
    }

    void syncSelectionFromTranscript() {
        AssistantProfile selected = selectionSync.selectFromTranscript();
        setAssistantProfileSelection(selected, false);
    }

    private void handleAssistantProfileSelection() {
        if (updatingSelection) {
            return;
        }
        AssistantProfile profile = (AssistantProfile) selector.getSelectedItem();
        if (profile == null) {
            return;
        }
        selectionSync.handleUserSelection(profile);
    }

    private void openAssistantProfileManager() {
        AssistantProfileManagerDialog dialog = new AssistantProfileManagerDialog(
            SwingUtilities.getWindowAncestor(panel),
            selectionModel);
        dialog.openDialog();
        AssistantProfile current = selectionModel.getSelectedProfile();
        selectionModel.reloadProfiles();
        if (current != null && current.isCustom()) {
            setAssistantProfileSelection(current, false);
        } else if (current != null) {
            selectionModel.selectById(current.getId());
            setAssistantProfileSelection(selectionModel.getSelectedProfile(), false);
        } else {
            setAssistantProfileSelection(selectionModel.getSelectedProfile(), false);
        }
    }

    private void setAssistantProfileSelection(AssistantProfile profile, boolean updateLastUsed) {
        updatingSelection = true;
        AssistantProfile resolved = profile == null ? AssistantProfile.defaultProfile() : profile;
        DefaultComboBoxModel<AssistantProfile> model = new DefaultComboBoxModel<>(
            buildAssistantProfileOptions(resolved).toArray(new AssistantProfile[0]));
        selector.setModel(model);
        selector.setSelectedItem(resolved);
        selectionModel.setSelectedProfile(resolved, updateLastUsed);
        updatingSelection = false;
    }

    private List<AssistantProfile> buildAssistantProfileOptions(AssistantProfile selected) {
        List<AssistantProfile> profiles = selectionModel.getProfiles();
        if (selected != null && selected.isCustom()) {
            profiles.add(1, selected);
        }
        return profiles;
    }
}
