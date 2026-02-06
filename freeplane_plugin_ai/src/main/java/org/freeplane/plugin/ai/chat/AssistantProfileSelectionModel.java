package org.freeplane.plugin.ai.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.freeplane.core.resources.ResourceController;

public class AssistantProfileSelectionModel {
    public static final String LAST_PROFILE_ID_PROPERTY = "ai_assistant_profile_last_id";

    private final AssistantProfileStore store;
    private final ResourceController resourceController;
    private final List<Consumer<AssistantProfile>> listeners = new ArrayList<>();
    private final List<AssistantProfile> profiles = new ArrayList<>();
    private AssistantProfile selectedProfile;

    public AssistantProfileSelectionModel() {
        this(new AssistantProfileStore(), ResourceController.getResourceController());
    }

    AssistantProfileSelectionModel(AssistantProfileStore store, ResourceController resourceController) {
        this.store = Objects.requireNonNull(store, "store");
        this.resourceController = Objects.requireNonNull(resourceController, "resourceController");
        reloadProfiles();
        selectById(resourceController.getProperty(LAST_PROFILE_ID_PROPERTY));
    }

    public void reloadProfiles() {
        profiles.clear();
        profiles.addAll(store.loadProfiles());
        ensureDefaultProfile();
    }

    public List<AssistantProfile> getProfiles() {
        return new ArrayList<>(profiles);
    }

    public AssistantProfile getSelectedProfile() {
        return selectedProfile;
    }

    public void addSelectionListener(Consumer<AssistantProfile> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void selectById(String id) {
        AssistantProfile profile = findById(id).orElse(AssistantProfile.defaultProfile());
        setSelectedProfile(profile, true);
    }

    public AssistantProfile selectByPrompt(String prompt) {
        AssistantProfile profile = findByPrompt(prompt).orElseGet(() -> {
            if (prompt == null || prompt.trim().isEmpty()) {
                return AssistantProfile.defaultProfile();
            }
            return AssistantProfile.customProfile(prompt);
        });
        setSelectedProfile(profile, false);
        return profile;
    }

    public void setSelectedProfile(AssistantProfile profile, boolean updateLastUsed) {
        this.selectedProfile = profile == null ? AssistantProfile.defaultProfile() : profile;
        if (updateLastUsed && !selectedProfile.isCustom()) {
            resourceController.setProperty(LAST_PROFILE_ID_PROPERTY, selectedProfile.getId());
        }
        notifyListeners();
    }

    public void saveProfiles(List<AssistantProfile> profilesToSave) {
        store.saveProfiles(profilesToSave);
        reloadProfiles();
    }

    private void ensureDefaultProfile() {
        AssistantProfile defaultProfile = profiles.stream()
            .filter(AssistantProfile::isDefault)
            .findFirst()
            .orElse(null);
        if (defaultProfile == null) {
            profiles.add(0, AssistantProfile.defaultProfile());
            return;
        }
        profiles.remove(defaultProfile);
        profiles.add(0, defaultProfile);
    }

    private Optional<AssistantProfile> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return profiles.stream()
            .filter(profile -> id.equals(profile.getId()))
            .findFirst();
    }

    private Optional<AssistantProfile> findByPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return Optional.empty();
        }
        return profiles.stream()
            .filter(profile -> prompt.equals(profile.getPrompt()))
            .findFirst();
    }

    private void notifyListeners() {
        for (Consumer<AssistantProfile> listener : listeners) {
            listener.accept(selectedProfile);
        }
    }
}
