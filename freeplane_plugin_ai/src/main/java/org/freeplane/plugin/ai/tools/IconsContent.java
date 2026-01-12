package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IconsContent {
    @Description("Icon descriptions for the node. Use listAvailableIcons to discover built-in and user icons. "
        + "Emoji icons are referenced by the emoji character itself and are not listed there.")
    private final List<String> descriptions;

    @JsonCreator
    public IconsContent(@JsonProperty("descriptions") List<String> descriptions) {
        this.descriptions = descriptions;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }
}
