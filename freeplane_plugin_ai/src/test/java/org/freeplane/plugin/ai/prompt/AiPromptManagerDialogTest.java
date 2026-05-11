package org.freeplane.plugin.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.Test;

public class AiPromptManagerDialogTest {
    private static final String DEFAULT_NEW_PROMPT_NAME = "New Prompt";

    @Test
    public void editorState_marksShowInChatChangesAsDirty() {
        AiPromptManagerDialog.EditorState state = new AiPromptManagerDialog.EditorState();
        state.loadSavedPrompts(Arrays.asList(new AiPrompt("Rewrite", "Prompt", false)));
        state.selectSavedPrompt(0);

        state.updateDraft("Rewrite", "Prompt", true);

        assertThat(state.isDirty()).isTrue();
    }

    @Test
    public void editorState_save_overwritesSelectedSavedPrompt() {
        AiPromptManagerDialog.EditorState state = new AiPromptManagerDialog.EditorState();
        state.loadSavedPrompts(Arrays.asList(new AiPrompt("Rewrite", "Prompt", false)));
        state.selectSavedPrompt(0);
        state.updateDraft("Rewrite better", "Other prompt", true);

        state.save(DEFAULT_NEW_PROMPT_NAME);

        assertThat(state.getSavedPrompts())
            .extracting(AiPrompt::getName, AiPrompt::getPrompt, AiPrompt::isShowInChat)
            .containsExactly(tuple("Rewrite better", "Other prompt", true));
        assertThat(state.getSelectedSavedPromptIndex()).isEqualTo(0);
        assertThat(state.isDirty()).isFalse();
    }

    @Test
    public void editorState_save_createsPromptForNewDraft() {
        AiPromptManagerDialog.EditorState state = new AiPromptManagerDialog.EditorState();
        state.loadSavedPrompts(Arrays.asList(new AiPrompt("Rewrite", "Prompt", false)));
        state.beginNewDraft();
        state.updateDraft("", "Summarize subtree", false);

        state.save(DEFAULT_NEW_PROMPT_NAME);

        assertThat(state.getSavedPrompts())
            .extracting(AiPrompt::getName)
            .containsExactly("Rewrite", "New Prompt");
        assertThat(state.getSelectedSavedPromptIndex()).isEqualTo(1);
    }

    @Test
    public void editorState_saveAsNew_keepsOriginalPromptAndAddsVariant() {
        AiPromptManagerDialog.EditorState state = new AiPromptManagerDialog.EditorState();
        state.loadSavedPrompts(Arrays.asList(new AiPrompt("Rewrite", "Prompt", false)));
        state.selectSavedPrompt(0);
        state.updateDraft("Rewrite", "Other prompt", true);

        state.saveAsNew(DEFAULT_NEW_PROMPT_NAME);

        assertThat(state.getSavedPrompts())
            .extracting(AiPrompt::getName, AiPrompt::getPrompt, AiPrompt::isShowInChat)
            .containsExactly(
                tuple("Rewrite", "Prompt", false),
                tuple("Rewrite 1", "Other prompt", true));
        assertThat(state.getSelectedSavedPromptIndex()).isEqualTo(1);
        assertThat(state.isDirty()).isFalse();
    }

    @Test
    public void editorState_saveAsNew_isDisabledForUnsavedDraft() {
        AiPromptManagerDialog.EditorState state = new AiPromptManagerDialog.EditorState();
        state.loadSavedPrompts(Arrays.asList(new AiPrompt("Rewrite", "Prompt", false)));
        state.beginNewDraft();

        assertThat(state.canSaveAsNew()).isFalse();
        assertThatThrownBy(() -> state.saveAsNew(DEFAULT_NEW_PROMPT_NAME))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void editorState_deleteSelectedPrompt_selectsNextSavedPrompt() {
        AiPromptManagerDialog.EditorState state = new AiPromptManagerDialog.EditorState();
        state.loadSavedPrompts(Arrays.asList(
            new AiPrompt("Rewrite", "Prompt", false),
            new AiPrompt("Summarize", "Other", true)));
        state.selectSavedPrompt(0);

        state.deleteSelectedPrompt();

        assertThat(state.getSavedPrompts())
            .extracting(AiPrompt::getName)
            .containsExactly("Summarize");
        assertThat(state.getSelectedSavedPromptIndex()).isEqualTo(0);
        assertThat(state.getCurrentDraft().getName()).isEqualTo("Summarize");
    }

    @Test
    public void editorState_restoreKeepsNewDraftWithEmptySelectedPromptName() {
        AiPromptManagerDialog.EditorState state = new AiPromptManagerDialog.EditorState();

        state.loadState(
            Arrays.asList(new AiPrompt("Rewrite", "Prompt", false)),
            new AiPromptStore.PersistedDialogState("", new AiPrompt("", "Draft", true)),
            DEFAULT_NEW_PROMPT_NAME);

        assertThat(state.getSelectedSavedPromptIndex()).isEqualTo(-1);
        assertThat(state.getCurrentDraft().getPrompt()).isEqualTo("Draft");
        assertThat(state.isDirty()).isTrue();
    }

    @Test
    public void editorState_restoreClearsMissingSelectedPromptNameAndKeepsDraft() {
        AiPromptManagerDialog.EditorState state = new AiPromptManagerDialog.EditorState();

        state.loadState(
            Arrays.asList(new AiPrompt("Rewrite", "Prompt", false)),
            new AiPromptStore.PersistedDialogState("Missing", new AiPrompt("Unsaved", "Draft", true)),
            DEFAULT_NEW_PROMPT_NAME);

        assertThat(state.getSelectedSavedPromptIndex()).isEqualTo(-1);
        assertThat(state.getCurrentDraft().getName()).isEqualTo("Unsaved");
        assertThat(state.getCurrentDraft().getPrompt()).isEqualTo("Draft");
        assertThat(state.isDirty()).isTrue();
    }

    @Test
    public void editorState_persistsEmptySelectedPromptNameForNewDraft() {
        AiPromptManagerDialog.EditorState state = new AiPromptManagerDialog.EditorState();
        state.loadSavedPrompts(Arrays.asList(new AiPrompt("Rewrite", "Prompt", false)));
        state.beginNewDraft();
        state.updateDraft("", "Draft", false);

        AiPromptStore.PersistedDialogState persistedDialogState = state.createPersistedDialogState();

        assertThat(persistedDialogState.getSelectedPromptName()).isEmpty();
        assertThat(persistedDialogState.getDraft().getPrompt()).isEqualTo("Draft");
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }
}
