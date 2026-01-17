package org.freeplane.plugin.ai.tools.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultItem {
    private final String nodeIdentifier;
    private final String briefText;
    private final String breadcrumbPath;

    @JsonCreator
    public SearchResultItem(@JsonProperty("nodeIdentifier") String nodeIdentifier,
                            @JsonProperty("briefText") String briefText,
                            @JsonProperty("breadcrumbPath") String breadcrumbPath) {
        this.nodeIdentifier = nodeIdentifier;
        this.briefText = briefText;
        this.breadcrumbPath = breadcrumbPath;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public String getBriefText() {
        return briefText;
    }

    public String getBreadcrumbPath() {
        return breadcrumbPath;
    }
}
