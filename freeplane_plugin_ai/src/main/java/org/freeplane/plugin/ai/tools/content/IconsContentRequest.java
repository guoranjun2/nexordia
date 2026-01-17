package org.freeplane.plugin.ai.tools.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IconsContentRequest {
    private final boolean includesIcons;

    @JsonCreator
    public IconsContentRequest(@JsonProperty("includesIcons") boolean includesIcons) {
        this.includesIcons = includesIcons;
    }

    public boolean includesIcons() {
        return includesIcons;
    }
}
