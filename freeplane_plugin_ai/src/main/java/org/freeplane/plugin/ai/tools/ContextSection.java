package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ContextSection {
    @JsonProperty("breadcrumb_path")
    BREADCRUMB_PATH,
    @JsonProperty("parent_summary")
    PARENT_SUMMARY,
    @JsonProperty("qualifiers")
    QUALIFIERS
}
