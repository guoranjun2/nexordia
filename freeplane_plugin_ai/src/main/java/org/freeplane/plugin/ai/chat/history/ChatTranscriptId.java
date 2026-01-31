package org.freeplane.plugin.ai.chat.history;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class ChatTranscriptId {
    private String fileName;

    public ChatTranscriptId() {
    }

    public ChatTranscriptId(String fileName) {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLeafFileName() {
        if (fileName == null) {
            return null;
        }
        Path path = Paths.get(fileName);
        Path leaf = path.getFileName();
        return leaf == null ? fileName : leaf.toString();
    }
}
