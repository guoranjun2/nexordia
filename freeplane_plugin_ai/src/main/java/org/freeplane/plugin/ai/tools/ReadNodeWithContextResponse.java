package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadNodeWithContextResponse {
    private final String mapIdentifier;
    private final NodeContentItem focusNode;
    private final NodeContentItem parentNode;
    private final List<NodeContentItem> childNodes;
    private final String breadcrumbPath;

    @JsonCreator
    public ReadNodeWithContextResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                       @JsonProperty("focusNode") NodeContentItem focusNode,
                                       @JsonProperty("parentNode") NodeContentItem parentNode,
                                       @JsonProperty("childNodes") List<NodeContentItem> childNodes,
                                       @JsonProperty("breadcrumbPath") String breadcrumbPath) {
        this.mapIdentifier = mapIdentifier;
        this.focusNode = focusNode;
        this.parentNode = parentNode;
        this.childNodes = childNodes;
        this.breadcrumbPath = breadcrumbPath;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public NodeContentItem getFocusNode() {
        return focusNode;
    }

    public NodeContentItem getParentNode() {
        return parentNode;
    }

    public List<NodeContentItem> getChildNodes() {
        return childNodes;
    }

    public String getBreadcrumbPath() {
        return breadcrumbPath;
    }
}
