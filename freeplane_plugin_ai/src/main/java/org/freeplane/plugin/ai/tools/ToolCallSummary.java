package org.freeplane.plugin.ai.tools;

import java.util.Objects;

public final class ToolCallSummary {
    private final String toolName;
    private final String summaryText;
    private final boolean hasError;

    public ToolCallSummary(String toolName, String summaryText, boolean hasError) {
        this.toolName = Objects.requireNonNull(toolName, "toolName");
        this.summaryText = Objects.requireNonNull(summaryText, "summaryText");
        this.hasError = hasError;
    }

    public String getToolName() {
        return toolName;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public boolean hasError() {
        return hasError;
    }
}
