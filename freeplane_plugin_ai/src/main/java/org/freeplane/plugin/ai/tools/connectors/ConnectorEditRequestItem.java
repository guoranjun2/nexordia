package org.freeplane.plugin.ai.tools.connectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import org.freeplane.plugin.ai.tools.edit.EditOperation;

public class ConnectorEditRequestItem {
    @Description("Source node identifier for the connector.")
    private final String sourceNodeIdentifier;
    @Description("Target node identifier for the connector.")
    private final String targetNodeIdentifier;
    private final EditOperation operation;
    private final String sourceLabel;
    private final String middleLabel;
    private final String targetLabel;
    private final String matchSourceLabel;
    private final String matchMiddleLabel;
    private final String matchTargetLabel;

    @JsonCreator
    public ConnectorEditRequestItem(@JsonProperty("sourceNodeIdentifier") String sourceNodeIdentifier,
                                    @JsonProperty("targetNodeIdentifier") String targetNodeIdentifier,
                                    @JsonProperty("operation") EditOperation operation,
                                    @JsonProperty("sourceLabel") String sourceLabel,
                                    @JsonProperty("middleLabel") String middleLabel,
                                    @JsonProperty("targetLabel") String targetLabel,
                                    @JsonProperty("matchSourceLabel") String matchSourceLabel,
                                    @JsonProperty("matchMiddleLabel") String matchMiddleLabel,
                                    @JsonProperty("matchTargetLabel") String matchTargetLabel) {
        this.sourceNodeIdentifier = sourceNodeIdentifier;
        this.targetNodeIdentifier = targetNodeIdentifier;
        this.operation = operation;
        this.sourceLabel = sourceLabel;
        this.middleLabel = middleLabel;
        this.targetLabel = targetLabel;
        this.matchSourceLabel = matchSourceLabel;
        this.matchMiddleLabel = matchMiddleLabel;
        this.matchTargetLabel = matchTargetLabel;
    }

    public String getSourceNodeIdentifier() {
        return sourceNodeIdentifier;
    }

    public String getTargetNodeIdentifier() {
        return targetNodeIdentifier;
    }

    public EditOperation getOperation() {
        return operation;
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

    public String getMatchSourceLabel() {
        return matchSourceLabel;
    }

    public String getMatchMiddleLabel() {
        return matchMiddleLabel;
    }

    public String getMatchTargetLabel() {
        return matchTargetLabel;
    }
}
