package org.freeplane.plugin.ai.tools;

@FunctionalInterface
public interface ToolCallSummaryHandler {
    void handleToolCallSummary(ToolCallSummary summary);
}
