package org.freeplane.plugin.ai.tools.tagcategories;

import java.util.Objects;
import java.util.UUID;

import org.freeplane.features.icon.TagCategoryAccess;
import org.freeplane.features.icon.TagCategorySnapshot;
import org.freeplane.features.map.MapModel;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummary;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummaryFormatter;

public class FetchTagCategoriesTool {
    private final AvailableMaps availableMaps;
    private final AvailableMaps.MapAccessListener mapAccessListener;
    private final TagCategoryAccess tagCategoryAccess;

    public FetchTagCategoriesTool(AvailableMaps availableMaps, AvailableMaps.MapAccessListener mapAccessListener,
                                  TagCategoryAccess tagCategoryAccess) {
        this.availableMaps = Objects.requireNonNull(availableMaps, "availableMaps");
        this.mapAccessListener = mapAccessListener;
        this.tagCategoryAccess = Objects.requireNonNull(tagCategoryAccess, "tagCategoryAccess");
    }

    public TagCategorySnapshotPayload fetchTagCategories(FetchTagCategoriesRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Missing request");
        }
        String mapIdentifierValue = requireValue(request.getMapIdentifier(), "mapIdentifier");
        UUID mapIdentifier = parseMapIdentifier(mapIdentifierValue);
        MapModel mapModel = availableMaps.findMapModel(mapIdentifier, mapAccessListener);
        if (mapModel == null) {
            throw new IllegalArgumentException("Unknown map identifier: " + mapIdentifierValue);
        }
        TagCategorySnapshot snapshot = tagCategoryAccess.readSnapshot(mapModel);
        return TagCategorySnapshotPayload.fromSnapshot(mapIdentifierValue, snapshot);
    }

    public ToolCallSummary buildToolCallSummary(FetchTagCategoriesRequest request, TagCategorySnapshotPayload response) {
        int categoryCount = response == null || response.getCategories() == null
            ? 0
            : response.getCategories().size();
        String summaryText = "fetchTagCategories: categories=" + categoryCount;
        String revision = response == null ? "" : ToolCallSummaryFormatter.sanitizeValue(response.getRevision());
        if (!revision.isEmpty()) {
            summaryText = summaryText + ", revision=" + revision;
        }
        return new ToolCallSummary("fetchTagCategories", summaryText, false);
    }

    public ToolCallSummary buildToolCallErrorSummary(FetchTagCategoriesRequest request, RuntimeException error) {
        String message = error == null ? "Unknown error" : error.getMessage();
        String safeMessage = ToolCallSummaryFormatter.sanitizeValue(message == null
            ? error.getClass().getSimpleName()
            : message);
        return new ToolCallSummary("fetchTagCategories", "fetchTagCategories error: " + safeMessage, true);
    }

    private String requireValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + fieldName + ".");
        }
        return value.trim();
    }

    private UUID parseMapIdentifier(String mapIdentifier) {
        try {
            return UUID.fromString(mapIdentifier);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid map identifier: " + mapIdentifier, error);
        }
    }
}
