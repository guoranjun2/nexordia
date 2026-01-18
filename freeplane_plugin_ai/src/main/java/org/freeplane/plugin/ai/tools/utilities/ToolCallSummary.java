package org.freeplane.plugin.ai.tools.utilities;

import java.util.Objects;

public class ToolCallSummary {
    private final String toolName;
    private final String summaryText;
    private final boolean hasError;
    private final ToolCaller toolCaller;

    public ToolCallSummary(String toolName, String summaryText, boolean hasError) {
        this(toolName, summaryText, hasError, ToolCaller.CHAT);
    }

    public ToolCallSummary(String toolName, String summaryText, boolean hasError, ToolCaller toolCaller) {
        this.toolName = Objects.requireNonNull(toolName, "toolName");
        this.summaryText = Objects.requireNonNull(summaryText, "summaryText");
        this.hasError = hasError;
        this.toolCaller = Objects.requireNonNull(toolCaller, "toolCaller");
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

    public ToolCaller getToolCaller() {
        return toolCaller;
    }

    public ToolCallSummary withToolCaller(ToolCaller newToolCaller) {
        if (toolCaller == newToolCaller) {
            return this;
        }
        return new ToolCallSummary(toolName, summaryText, hasError, newToolCaller);
    }
}
