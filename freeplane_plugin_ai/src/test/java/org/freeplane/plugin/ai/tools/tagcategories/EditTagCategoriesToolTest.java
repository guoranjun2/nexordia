package org.freeplane.plugin.ai.tools.tagcategories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import javax.swing.tree.DefaultMutableTreeNode;

import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.TagCategoryAccess;
import org.freeplane.features.icon.TagCategoryConflictException;
import org.freeplane.features.icon.TagCategoryEdit;
import org.freeplane.features.icon.TagCategoryEditBatch;
import org.freeplane.features.icon.TagCategoryEditType;
import org.freeplane.features.icon.TagCategorySnapshot;
import org.freeplane.features.icon.TagCategorySnapshotBuilder;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.mindmapmode.FreeplaneTagCategoryAccess;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.map.MapModel;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class EditTagCategoriesToolTest {
    @Test
    public void editTagCategoriesConvertsOrderedOperationsAndReturnsSnapshot() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
        MapModel mapModel = mock(MapModel.class);
        String mapIdentifier = UUID.randomUUID().toString();
        UUID parsedMapIdentifier = UUID.fromString(mapIdentifier);
        when(availableMaps.findMapModel(parsedMapIdentifier, null)).thenReturn(mapModel);
        TagCategorySnapshot appliedSnapshot = new TagCategorySnapshot(
            "sha256:applied",
            "/",
            Collections.emptyList(),
            Collections.emptyList());
        when(tagCategoryAccess.applyEdits(eq(mapModel), any())).thenReturn(appliedSnapshot);
        EditTagCategoriesTool uut = new EditTagCategoriesTool(availableMaps, null, tagCategoryAccess);
        TagCategoryEditPayload renameOperation = new TagCategoryEditPayload(
            TagCategoryEditType.RENAME,
            Arrays.asList("Project", "Status"),
            "State",
            null,
            null,
            null,
            null);
        TagCategoryEditPayload separatorOperation = new TagCategoryEditPayload(
            TagCategoryEditType.SET_SEPARATOR,
            null,
            null,
            null,
            null,
            null,
            "/");
        TagCategoryEditBatchPayload request = new TagCategoryEditBatchPayload(
            mapIdentifier,
            "sha256:expected",
            Arrays.asList(renameOperation, separatorOperation));

        TagCategorySnapshotPayload result = uut.editTagCategories(request);

        ArgumentCaptor<TagCategoryEditBatch> batchCaptor = ArgumentCaptor.forClass(TagCategoryEditBatch.class);
        verify(tagCategoryAccess).applyEdits(eq(mapModel), batchCaptor.capture());
        TagCategoryEditBatch submittedBatch = batchCaptor.getValue();
        assertThat(submittedBatch.getExpectedRevision()).isEqualTo("sha256:expected");
        assertThat(submittedBatch.getOperations()).extracting(TagCategoryEdit::getType)
            .containsExactly(TagCategoryEditType.RENAME, TagCategoryEditType.SET_SEPARATOR);
        assertThat(submittedBatch.getOperations().get(0).getPath())
            .containsExactly("Project", "Status");
        assertThat(submittedBatch.getOperations().get(0).getNewName()).isEqualTo("State");
        assertThat(submittedBatch.getOperations().get(1).getNewSeparator()).isEqualTo("/");
        assertThat(result.getMapIdentifier()).isEqualTo(mapIdentifier);
        assertThat(result.getRevision()).isEqualTo("sha256:applied");
        assertThat(result.getSeparator()).isEqualTo("/");
    }

    @Test
    public void editTagCategoriesPropagatesStaleRevisionConflict() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
        MapModel mapModel = mock(MapModel.class);
        String mapIdentifier = UUID.randomUUID().toString();
        UUID parsedMapIdentifier = UUID.fromString(mapIdentifier);
        when(availableMaps.findMapModel(parsedMapIdentifier, null)).thenReturn(mapModel);
        when(tagCategoryAccess.applyEdits(eq(mapModel), any()))
            .thenThrow(new TagCategoryConflictException("stale revision"));
        EditTagCategoriesTool uut = new EditTagCategoriesTool(availableMaps, null, tagCategoryAccess);
        TagCategoryEditPayload separatorOperation = new TagCategoryEditPayload(
            TagCategoryEditType.SET_SEPARATOR,
            null,
            null,
            null,
            null,
            null,
            "/");
        TagCategoryEditBatchPayload request = new TagCategoryEditBatchPayload(
            mapIdentifier,
            "sha256:expected",
            Collections.singletonList(separatorOperation));

        assertThatThrownBy(() -> uut.editTagCategories(request))
            .isInstanceOf(TagCategoryConflictException.class)
            .hasMessageContaining("stale revision");
    }

    @Test
    public void editTagCategoriesMatchesCoreAccessOutcomeForEquivalentPayload() {
        String mapIdentifier = UUID.randomUUID().toString();
        MapModel mapModelForTool = createMapModelWithProjectStatusCategory();
        MapModel mapModelForDirectApply = createMapModelWithProjectStatusCategory();
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        when(availableMaps.findMapModel(UUID.fromString(mapIdentifier), null)).thenReturn(mapModelForTool);
        MIconController iconController = mock(MIconController.class);
        FreeplaneTagCategoryAccess access = new FreeplaneTagCategoryAccess(iconController);
        EditTagCategoriesTool uut = new EditTagCategoriesTool(availableMaps, null, access);
        String expectedRevision = TagCategorySnapshotBuilder
            .from(mapModelForTool.getIconRegistry().getTagCategories())
            .getRevision();
        TagCategoryEditPayload renameOperation = new TagCategoryEditPayload(
            TagCategoryEditType.RENAME,
            Arrays.asList("Project", "Status"),
            "State",
            null,
            null,
            null,
            null);
        TagCategoryEditPayload separatorOperation = new TagCategoryEditPayload(
            TagCategoryEditType.SET_SEPARATOR,
            null,
            null,
            null,
            null,
            null,
            "/");
        TagCategoryEditBatchPayload request = new TagCategoryEditBatchPayload(
            mapIdentifier,
            expectedRevision,
            Arrays.asList(renameOperation, separatorOperation));
        TagCategoryEditBatch directBatch = new TagCategoryEditBatch(
            expectedRevision,
            Arrays.asList(
                TagCategoryEdit.rename(Arrays.asList("Project", "Status"), "State"),
                TagCategoryEdit.setSeparator("/")));
        TagCategorySnapshot expectedSnapshot = access.applyEdits(mapModelForDirectApply, directBatch);

        TagCategorySnapshotPayload actualPayload = uut.editTagCategories(request);

        assertThat(actualPayload.toSnapshot()).isEqualTo(expectedSnapshot);
    }

    private MapModel createMapModelWithProjectStatusCategory() {
        TagCategories tagCategories = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        tagCategories.load("Project\n"
            + " Status\n");
        return new MapModel(
            (source, targetMap, withChildren) -> null,
            new IconRegistry(tagCategories),
            null);
    }
}
