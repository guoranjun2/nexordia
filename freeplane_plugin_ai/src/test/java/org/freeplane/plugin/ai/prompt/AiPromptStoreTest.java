package org.freeplane.plugin.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import org.junit.Test;

public class AiPromptStoreTest {

    @Test
    public void saveAndLoad_preservesPromptsAndDialogState() throws IOException {
        Path tempDir = Files.createTempDirectory("ai-prompts");
        try {
            Path path = tempDir.resolve(AiPromptStore.PROMPTS_FILE_NAME);
            AiPromptStore store = new AiPromptStore(new ObjectMapper(), path);
            AiPromptStore.PersistedState state = new AiPromptStore.PersistedState(
                Arrays.asList(
                    new AiPrompt("Rewrite", "Rewrite node", true),
                    new AiPrompt("Summarize", "Summarize subtree", false)),
                new AiPromptStore.PersistedDialogState(
                    "",
                    new AiPrompt("", "Draft prompt", true)));

            store.saveState(state);
            AiPromptStore.PersistedState loaded = store.loadState();

            assertThat(loaded.getSavedPrompts())
                .extracting(AiPrompt::getName, AiPrompt::getPrompt, AiPrompt::isShowInChat)
                .containsExactly(
                    tuple("Rewrite", "Rewrite node", true),
                    tuple("Summarize", "Summarize subtree", false));
            assertThat(loaded.getDialogState().getSelectedPromptName()).isEmpty();
            assertThat(loaded.getDialogState().getDraft().getPrompt()).isEqualTo("Draft prompt");
            assertThat(loaded.getDialogState().getDraft().isShowInChat()).isTrue();
        }
        finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void loadState_returnsEmptyStateForObsoleteArrayShape() throws IOException {
        Path tempDir = Files.createTempDirectory("ai-prompts");
        try {
            Path path = tempDir.resolve(AiPromptStore.PROMPTS_FILE_NAME);
            Files.write(
                path,
                "[{\"name\":\"Rewrite\",\"prompt\":\"Prompt\",\"showInChat\":true}]".getBytes(StandardCharsets.UTF_8));
            AiPromptStore store = new AiPromptStore(new ObjectMapper(), path);

            AiPromptStore.PersistedState loaded = store.loadState();

            assertThat(loaded.getSavedPrompts()).isEmpty();
            assertThat(loaded.getDialogState().getSelectedPromptName()).isEmpty();
            assertThat(loaded.getDialogState().getDraft().getName()).isEmpty();
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

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }
}
