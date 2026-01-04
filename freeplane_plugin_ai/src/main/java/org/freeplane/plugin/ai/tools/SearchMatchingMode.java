package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SearchMatchingMode {
    @JsonProperty("contains")
    CONTAINS,
    @JsonProperty("equals")
    EQUALS,
    @JsonProperty("regular_expression")
    REGULAR_EXPRESSION
}
