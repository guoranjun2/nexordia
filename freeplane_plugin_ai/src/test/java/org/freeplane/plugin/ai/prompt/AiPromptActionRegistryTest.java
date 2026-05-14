package org.freeplane.plugin.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import org.freeplane.plugin.ai.chat.AIChatPanel;
import org.freeplane.plugin.ai.prompt.ui.AiPromptManagerDialog;
import org.junit.Test;

public class AiPromptActionRegistryTest {

    @Test
    public void actionKey_usesTrimmedPromptName() {
        assertThat(AiPromptActionRegistry.actionKey("  Rewrite node  "))
            .isEqualTo("RunAiPromptAction.Rewrite node");
    }

    @Test
    public void persistStateIfChanged_doesNotCreateFileForUnchangedEmptyState() throws IOException {
        Path tempDir = Files.createTempDirectory("ai-prompts");
        try {
            Path path = tempDir.resolve(AiPromptStore.PROMPTS_FILE_NAME);
            AiPromptActionRegistry registry = new AiPromptActionRegistry(
                new AiPromptStore(new ObjectMapper(), path),
                mock(AIChatPanel.class),
                () -> {
                });

            registry.persistStateIfChanged();

            assertThat(Files.exists(path)).isFalse();
        }
        finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void persistStateIfChanged_createsFileForChangedDialogState() throws IOException {
        Path tempDir = Files.createTempDirectory("ai-prompts");
        try {
            Path path = tempDir.resolve(AiPromptStore.PROMPTS_FILE_NAME);
            AiPromptActionRegistry registry = new AiPromptActionRegistry(
                new AiPromptStore(new ObjectMapper(), path),
                mock(AIChatPanel.class),
                () -> {
                });
            registry.getDialogState().loadSavedPrompts(Arrays.asList(new AiPrompt("Rewrite", "Prompt", false)));
            registry.getDialogState().beginNewDraft();
            registry.getDialogState().updateDraft("", "Draft prompt", true, "openrouter|openai/gpt-4.1-mini");

            registry.persistStateIfChanged();

            assertThat(Files.exists(path)).isTrue();
            AiPromptStore.PersistedState loaded = new AiPromptStore(new ObjectMapper(), path).loadState();
            assertThat(loaded.getDialogState().getSelectedPromptName()).isEmpty();
            assertThat(loaded.getDialogState().getDraft().getPrompt()).isEqualTo("Draft prompt");
            assertThat(loaded.getDialogState().getDraft().isShowInChat()).isTrue();
            assertThat(loaded.getDialogState().getDraft().getModelSelectionValue())
                .isEqualTo("openrouter|openai/gpt-4.1-mini");
        }
        finally {
            deleteRecursively(tempDir);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
