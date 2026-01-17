package org.freeplane.plugin.ai.tools;

import java.util.Objects;
import java.util.UUID;

import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.maps.AvailableMaps;

public class SelectedMapAndNodeIdentifiersTool {
    private final AvailableMaps availableMaps;
    private final SelectionIdentifiersBuilder selectionIdentifiersBuilder;

    public SelectedMapAndNodeIdentifiersTool(AvailableMaps availableMaps, TextController textController) {
        this.availableMaps = Objects.requireNonNull(availableMaps, "availableMaps");
        this.selectionIdentifiersBuilder = new SelectionIdentifiersBuilder(textController);
    }

    public SelectionIdentifiersResponse getSelectedMapAndNodeIdentifiers(SelectionIdentifiersRequest request) {
        UUID mapIdentifier = availableMaps.getCurrentMapIdentifier();
        MapModel mapModel = availableMaps.getCurrentMapModel();
        String mapIdentifierValue = mapIdentifier == null ? null : mapIdentifier.toString();
        IMapSelection selection = Controller.getCurrentController().getSelection();
        SelectionCollectionMode selectionCollectionMode = request == null
            ? null
            : request.getSelectionCollectionMode();
        return selectionIdentifiersBuilder.buildSelectionIdentifiersResponse(
            mapIdentifierValue, mapModel, selection, selectionCollectionMode);
    }

    ToolCallSummary buildToolCallSummary(SelectionIdentifiersResponse response) {
        return new ToolCallSummary("getSelectedMapAndNodeIdentifiers",
            "getSelectedMapAndNodeIdentifiers: selected identifiers read", false);
    }

    ToolCallSummary buildToolCallErrorSummary(RuntimeException error) {
        String message = error == null ? "Unknown error" : error.getMessage();
        String safeMessage = ToolCallSummaryFormatter.sanitizeValue(message == null
            ? error.getClass().getSimpleName()
            : message);
        return new ToolCallSummary("getSelectedMapAndNodeIdentifiers",
            "getSelectedMapAndNodeIdentifiers error: " + safeMessage, true);
    }
}
