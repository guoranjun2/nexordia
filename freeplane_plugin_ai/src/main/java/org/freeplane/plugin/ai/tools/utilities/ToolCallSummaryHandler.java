package org.freeplane.plugin.ai.tools.utilities;

@FunctionalInterface
public interface ToolCallSummaryHandler {
    void handleToolCallSummary(ToolCallSummary summary);
}
