package org.freeplane.plugin.ai.tools.content;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListAvailableIconsResponse {
    private final List<String> icons;
    private final String note;

    @JsonCreator
    public ListAvailableIconsResponse(@JsonProperty("icons") List<String> icons,
                                      @JsonProperty("note") String note) {
        this.icons = icons;
        this.note = note;
    }

    public List<String> getIcons() {
        return icons;
    }

    public String getNote() {
        return note;
    }
}
