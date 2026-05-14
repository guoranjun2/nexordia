package org.freeplane.plugin.ai.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.LogUtils;

public class AiPromptStore {
    static final String PROMPTS_FILE_NAME = "ai-prompts.json";

    private final ObjectMapper objectMapper;
    private final Path promptsPath;

    AiPromptStore() {
        this(new ObjectMapper(), resolveDefaultPath());
    }

    AiPromptStore(ObjectMapper objectMapper, Path promptsPath) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.promptsPath = Objects.requireNonNull(promptsPath, "promptsPath");
    }

    PersistedState loadState() {
        if (!Files.exists(promptsPath)) {
            return emptyState();
        }
        try (InputStream inputStream = Files.newInputStream(promptsPath)) {
            PersistedState loaded = objectMapper.readValue(inputStream, PersistedState.class);
            return loaded == null ? emptyState() : loaded.copy();
        }
        catch (IOException error) {
            LogUtils.severe(error);
            return emptyState();
        }
    }

    void saveState(PersistedState state) {
        ensureParentDirectory();
        try (OutputStream outputStream = Files.newOutputStream(promptsPath)) {
            objectMapper.writeValue(outputStream, state == null ? emptyState() : state);
        }
        catch (IOException error) {
            LogUtils.severe(error);
        }
    }

    static PersistedState emptyState() {
        return new PersistedState();
    }

    private void ensureParentDirectory() {
        Path parent = promptsPath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        }
        catch (IOException error) {
            LogUtils.severe(error);
        }
    }

    private static Path resolveDefaultPath() {
        String userDirectory = ResourceController.getResourceController().getFreeplaneUserDirectory();
        return Paths.get(userDirectory).resolve(PROMPTS_FILE_NAME);
    }

    public static class PersistedState {
        private List<AiPrompt> savedPrompts = new ArrayList<AiPrompt>();
        private PersistedDialogState dialogState = new PersistedDialogState();

        public PersistedState() {
        }

        PersistedState(List<AiPrompt> savedPrompts, PersistedDialogState dialogState) {
            setSavedPrompts(savedPrompts);
            setDialogState(dialogState);
        }

        public List<AiPrompt> getSavedPrompts() {
            return copyPrompts(savedPrompts);
        }

        public void setSavedPrompts(List<AiPrompt> savedPrompts) {
            this.savedPrompts = copyPrompts(savedPrompts);
        }

        public PersistedDialogState getDialogState() {
            return dialogState == null ? new PersistedDialogState() : dialogState.copy();
        }

        public void setDialogState(PersistedDialogState dialogState) {
            this.dialogState = dialogState == null ? new PersistedDialogState() : dialogState.copy();
        }

        PersistedState copy() {
            return new PersistedState(savedPrompts, dialogState);
        }
    }

    public static class PersistedDialogState {
        private String selectedPromptName = "";
        private AiPrompt draft = new AiPrompt("", "", false);

        public PersistedDialogState() {
        }

        public PersistedDialogState(String selectedPromptName, AiPrompt draft) {
            setSelectedPromptName(selectedPromptName);
            setDraft(draft);
        }

        public String getSelectedPromptName() {
            return selectedPromptName == null ? "" : selectedPromptName;
        }

        public void setSelectedPromptName(String selectedPromptName) {
            this.selectedPromptName = selectedPromptName == null ? "" : selectedPromptName;
        }

        public AiPrompt getDraft() {
            return draft == null ? new AiPrompt("", "", false) : draft.copy();
        }

        public void setDraft(AiPrompt draft) {
            this.draft = draft == null ? new AiPrompt("", "", false) : draft.copy();
        }

        PersistedDialogState copy() {
            return new PersistedDialogState(selectedPromptName, draft);
        }
    }

    private static List<AiPrompt> copyPrompts(List<AiPrompt> prompts) {
        List<AiPrompt> copies = new ArrayList<AiPrompt>();
        if (prompts == null) {
            return copies;
        }
        for (AiPrompt prompt : prompts) {
            copies.add(prompt == null ? new AiPrompt() : prompt.copy());
        }
        return copies;
    }
}
