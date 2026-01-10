package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchPropertyDefinition {
    private final String name;

    @JsonCreator
    public SearchPropertyDefinition(@JsonProperty("name") String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
