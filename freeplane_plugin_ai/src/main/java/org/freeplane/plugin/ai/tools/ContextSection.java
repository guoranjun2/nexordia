package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ContextSection {
    @JsonProperty("breadcrumb_path")
    BREADCRUMB_PATH,
    @JsonProperty("parent_summary")
    PARENT_SUMMARY,
    @JsonProperty("focus_content")
    FOCUS_CONTENT,
    @JsonProperty("child_summaries")
    CHILD_SUMMARIES
}
