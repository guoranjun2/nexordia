package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public final class FlatListItem {
    private final String nodeIdentifier;
    private final NodeContent content;
    private final List<BreadcrumbItem> breadcrumbs;

    @JsonCreator
    public FlatListItem(@JsonProperty("nodeIdentifier") String nodeIdentifier,
                        @JsonProperty("content") NodeContent content,
                        @JsonProperty("breadcrumbs") List<BreadcrumbItem> breadcrumbs) {
        this.nodeIdentifier = nodeIdentifier;
        this.content = content;
        this.breadcrumbs = breadcrumbs;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public NodeContent getContent() {
        return content;
    }

    public List<BreadcrumbItem> getBreadcrumbs() {
        return breadcrumbs;
    }
}
