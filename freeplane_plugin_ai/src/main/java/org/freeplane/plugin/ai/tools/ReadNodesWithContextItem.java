package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadNodesWithContextItem {
    private final List<NodeDepthItem> nodes;
    private final NodeContentItem parentNode;
    private final String breadcrumbPath;
    private final Omissions childOmissions;

    @JsonCreator
    public ReadNodesWithContextItem(@JsonProperty("nodes") List<NodeDepthItem> nodes,
                                    @JsonProperty("parentNode") NodeContentItem parentNode,
                                    @JsonProperty("breadcrumbPath") String breadcrumbPath,
                                    @JsonProperty("childOmissions") Omissions childOmissions) {
        this.nodes = nodes;
        this.parentNode = parentNode;
        this.breadcrumbPath = breadcrumbPath;
        this.childOmissions = childOmissions;
    }

    public List<NodeDepthItem> getNodes() {
        return nodes;
    }

    public NodeContentItem getParentNode() {
        return parentNode;
    }

    public String getBreadcrumbPath() {
        return breadcrumbPath;
    }

    public Omissions getChildOmissions() {
        return childOmissions;
    }
}
