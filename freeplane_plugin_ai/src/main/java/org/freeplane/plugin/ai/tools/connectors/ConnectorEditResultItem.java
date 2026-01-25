package org.freeplane.plugin.ai.tools.connectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.freeplane.plugin.ai.tools.content.ConnectorItem;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorEditResultItem {
    private final String sourceNodeIdentifier;
    private final String targetNodeIdentifier;
    private final String action;
    private final int ignoredAmbiguousConnectorCount;
    private final ConnectorItem connector;

    @JsonCreator
    public ConnectorEditResultItem(@JsonProperty("sourceNodeIdentifier") String sourceNodeIdentifier,
                                   @JsonProperty("targetNodeIdentifier") String targetNodeIdentifier,
                                   @JsonProperty("action") String action,
                                   @JsonProperty("ignoredAmbiguousConnectorCount") int ignoredAmbiguousConnectorCount,
                                   @JsonProperty("connector") ConnectorItem connector) {
        this.sourceNodeIdentifier = sourceNodeIdentifier;
        this.targetNodeIdentifier = targetNodeIdentifier;
        this.action = action;
        this.ignoredAmbiguousConnectorCount = ignoredAmbiguousConnectorCount;
        this.connector = connector;
    }

    public String getSourceNodeIdentifier() {
        return sourceNodeIdentifier;
    }

    public String getTargetNodeIdentifier() {
        return targetNodeIdentifier;
    }

    public String getAction() {
        return action;
    }

    public int getIgnoredAmbiguousConnectorCount() {
        return ignoredAmbiguousConnectorCount;
    }

    public ConnectorItem getConnector() {
        return connector;
    }
}
