package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IconsContent {
    private final List<String> descriptions;

    @JsonCreator
    public IconsContent(@JsonProperty("descriptions") List<String> descriptions) {
        this.descriptions = descriptions;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }
}
