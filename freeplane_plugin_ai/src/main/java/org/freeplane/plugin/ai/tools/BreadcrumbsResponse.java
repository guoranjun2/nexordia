package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class BreadcrumbsResponse {
    private final String mapIdentifier;
    private final List<BreadcrumbItem> breadcrumbs;

    public BreadcrumbsResponse(String mapIdentifier, List<BreadcrumbItem> breadcrumbs) {
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
