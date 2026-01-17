package org.freeplane.plugin.ai.tools.read;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BreadcrumbsResponse {
    private final String mapIdentifier;
    private final List<BreadcrumbItem> breadcrumbs;

    @JsonCreator
    public BreadcrumbsResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                               @JsonProperty("breadcrumbs") List<BreadcrumbItem> breadcrumbs) {
        this.mapIdentifier = mapIdentifier;
        this.breadcrumbs = breadcrumbs;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<BreadcrumbItem> getBreadcrumbs() {
        return breadcrumbs;
    }
}
