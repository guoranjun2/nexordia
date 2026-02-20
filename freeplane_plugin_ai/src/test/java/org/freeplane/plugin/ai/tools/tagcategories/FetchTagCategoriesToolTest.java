package org.freeplane.plugin.ai.tools.tagcategories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.freeplane.features.icon.TagCategoryAccess;
import org.freeplane.features.icon.TagCategoryNode;
import org.freeplane.features.icon.TagCategorySnapshot;
import org.freeplane.features.icon.TagDescriptor;
import org.freeplane.features.map.MapModel;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.junit.Test;

public class FetchTagCategoriesToolTest {
    @Test
    public void fetchTagCategoriesReturnsSnapshotPayloadWithRevisionAndTree() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
        MapModel mapModel = mock(MapModel.class);
        String mapIdentifier = UUID.randomUUID().toString();
        UUID parsedMapIdentifier = UUID.fromString(mapIdentifier);
        when(availableMaps.findMapModel(parsedMapIdentifier, null)).thenReturn(mapModel);
        TagCategorySnapshot snapshot = new TagCategorySnapshot(
            "sha256:test",
            "::",
            Collections.singletonList(new TagCategoryNode(
                Collections.singletonList("Project"),
                "Project",
                "Project",
                "#11223344",
                Collections.singletonList(new TagCategoryNode(
                    Arrays.asList("Project", "Status"),
                    "Status",
                    "Project::Status",
                    "#22334455",
                    Collections.emptyList())))),
            Collections.singletonList(new TagDescriptor(
                Collections.singletonList("urgent"),
                "urgent",
                "urgent",
                "#ff0000ff")));
        when(tagCategoryAccess.readSnapshot(mapModel)).thenReturn(snapshot);
        FetchTagCategoriesTool uut = new FetchTagCategoriesTool(availableMaps, null, tagCategoryAccess);

        TagCategorySnapshotPayload result = uut.fetchTagCategories(new FetchTagCategoriesRequest(mapIdentifier));

        assertThat(result.getMapIdentifier()).isEqualTo(mapIdentifier);
        assertThat(result.getRevision()).isEqualTo("sha256:test");
        assertThat(result.getSeparator()).isEqualTo("::");
        assertThat(result.getCategories()).hasSize(1);
        assertThat(result.getCategories().get(0).getQualifiedName()).isEqualTo("Project");
        assertThat(result.getCategories().get(0).getChildren()).hasSize(1);
        assertThat(result.getCategories().get(0).getChildren().get(0).getQualifiedName())
            .isEqualTo("Project::Status");
        verify(tagCategoryAccess).readSnapshot(mapModel);
    }

    @Test
    public void fetchTagCategoriesRejectsUnknownMapIdentifier() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
        FetchTagCategoriesTool uut = new FetchTagCategoriesTool(availableMaps, null, tagCategoryAccess);
        String mapIdentifier = UUID.randomUUID().toString();

        assertThatThrownBy(() -> uut.fetchTagCategories(new FetchTagCategoriesRequest(mapIdentifier)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown map identifier");
    }

    @Test
    public void fetchTagCategoriesRejectsInvalidMapIdentifier() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
        FetchTagCategoriesTool uut = new FetchTagCategoriesTool(availableMaps, null, tagCategoryAccess);

        assertThatThrownBy(() -> uut.fetchTagCategories(new FetchTagCategoriesRequest("invalid")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid map identifier");
    }
}
