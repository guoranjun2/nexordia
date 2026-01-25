package org.freeplane.plugin.ai.tools.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.freeplane.features.link.ConnectorModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorItem {
    private final String sourceNodeIdentifier;
    private final String targetNodeIdentifier;
    private final String sourceLabel;
    private final String middleLabel;
    private final String targetLabel;

    @JsonCreator
    public ConnectorItem(@JsonProperty("sourceNodeIdentifier") String sourceNodeIdentifier,
                         @JsonProperty("targetNodeIdentifier") String targetNodeIdentifier,
                         @JsonProperty("sourceLabel") String sourceLabel,
                         @JsonProperty("middleLabel") String middleLabel,
                         @JsonProperty("targetLabel") String targetLabel) {
        this.sourceNodeIdentifier = sourceNodeIdentifier;
        this.targetNodeIdentifier = targetNodeIdentifier;
        this.sourceLabel = sourceLabel;
        this.middleLabel = middleLabel;
        this.targetLabel = targetLabel;
    }

    public static ConnectorItem fromConnector(ConnectorModel connectorModel) {
        if (connectorModel == null) {
            return null;
        }
        String sourceIdentifier = connectorModel.getSource() == null ? null : connectorModel.getSource().createID();
        return new ConnectorItem(
            sourceIdentifier,
            connectorModel.getTargetID(),
            connectorModel.getSourceLabel().orElse(null),
            connectorModel.getMiddleLabel().orElse(null),
            connectorModel.getTargetLabel().orElse(null));
    }

    public String getSourceNodeIdentifier() {
        return sourceNodeIdentifier;
    }

    public String getTargetNodeIdentifier() {
        return targetNodeIdentifier;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getMiddleLabel() {
        return middleLabel;
    }

    public String getTargetLabel() {
        return targetLabel;
    }
}
