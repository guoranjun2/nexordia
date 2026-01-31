package org.freeplane.plugin.ai.chat.history;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ChatTranscriptStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void saveAndLoad_roundTripsEntriesAndMapCounts() throws IOException {
        ChatTranscriptStore uut = createStore();
        ChatTranscriptRecord record = new ChatTranscriptRecord();
        record.setDisplayName("First chat");
        record.setEntries(Arrays.asList(
            new ChatTranscriptEntry(ChatTranscriptRole.USER, "hello"),
            new ChatTranscriptEntry(ChatTranscriptRole.ASSISTANT, "hi there")));
        record.setMapRootShortTextCounts(Arrays.asList(
            new MapRootShortTextCount("Map A", 1),
            new MapRootShortTextCount("Map B", 2)));

        ChatTranscriptId id = uut.save(record, null);
        ChatTranscriptRecord loaded = uut.load(id);

        assertThat(id).isNotNull();
        assertThat(loaded).isNotNull();
        assertThat(loaded.getDisplayName()).isEqualTo("First chat");
        assertThat(loaded.getEntries()).hasSize(2);
        assertThat(loaded.getEntries().get(0).getRole()).isEqualTo(ChatTranscriptRole.USER);
        assertThat(loaded.getEntries().get(1).getRole()).isEqualTo(ChatTranscriptRole.ASSISTANT);
        assertThat(loaded.getMapRootShortTextCounts()).hasSize(2);
        assertThat(loaded.getMapRootShortTextCounts().get(1).getText()).isEqualTo("Map B");
    }

    @Test
    public void list_sortsByTimestampAndMarksMalformedFiles() throws IOException, InterruptedException {
        ChatTranscriptStore uut = createStore();
        ChatTranscriptRecord first = new ChatTranscriptRecord();
        first.setDisplayName("First");
        ChatTranscriptId firstId = uut.save(first, null);
        Thread.sleep(5L);
        ChatTranscriptRecord second = new ChatTranscriptRecord();
        second.setDisplayName("Second");
        ChatTranscriptId secondId = uut.save(second, null);
        createMalformedTranscriptFile();

        List<ChatTranscriptSummary> summaries = uut.list();

        assertThat(summaries).hasSize(3);
        assertThat(summaries.get(0).getTimestamp()).isGreaterThanOrEqualTo(summaries.get(1).getTimestamp());
        assertThat(summaries)
            .anySatisfy(summary -> {
                assertThat(summary.getStatus()).isEqualTo(ChatTranscriptStatus.TRANSCRIPT);
                assertThat(summary.getId().getFileName()).isIn(firstId.getFileName(), secondId.getFileName());
            })
            .anySatisfy(summary -> {
                assertThat(summary.getStatus()).isEqualTo(ChatTranscriptStatus.ERROR);
                assertThat(summary.getErrorMessage()).isNotBlank();
            });
    }

    @Test
    public void rename_updatesDisplayNameAndPreservesLeafFileName() throws IOException {
        ChatTranscriptStore uut = createStore();
        ChatTranscriptRecord record = new ChatTranscriptRecord();
        record.setDisplayName("Before");
        ChatTranscriptId id = uut.save(record, null);
        long originalTimestamp = uut.load(id).getTimestamp();

        ChatTranscriptId renamed = uut.rename(id, "After");
        ChatTranscriptRecord loaded = uut.load(renamed);

        assertThat(renamed.getLeafFileName()).isEqualTo(id.getLeafFileName());
        assertThat(loaded.getDisplayName()).isEqualTo("After");
        assertThat(loaded.getTimestamp()).isGreaterThanOrEqualTo(originalTimestamp);
    }

    @Test
    public void delete_removesTranscriptFile() throws IOException {
        ChatTranscriptStore uut = createStore();
        ChatTranscriptRecord record = new ChatTranscriptRecord();
        record.setDisplayName("Delete me");
        ChatTranscriptId id = uut.save(record, null);

        boolean deleted = uut.delete(id);

        assertThat(deleted).isTrue();
        assertThat(uut.load(id)).isNull();
        assertThat(uut.list()).isEmpty();
    }

    private ChatTranscriptStore createStore() throws IOException {
        Path root = temporaryFolder.newFolder("ai-chats").toPath();
        return new ChatTranscriptStore(new ObjectMapper(), root);
    }

    private void createMalformedTranscriptFile() throws IOException {
        Path root = temporaryFolder.getRoot().toPath().resolve("ai-chats").resolve("2000-01-01");
        Files.createDirectories(root);
        Path target = root.resolve("bad.json.gz");
        Files.write(target, "not-gzip".getBytes(StandardCharsets.US_ASCII));
    }
}
