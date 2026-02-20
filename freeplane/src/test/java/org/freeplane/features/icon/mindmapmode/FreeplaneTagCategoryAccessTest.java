package org.freeplane.features.icon.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.TagCategoryConflictException;
import org.freeplane.features.icon.TagCategoryEdit;
import org.freeplane.features.icon.TagCategoryEditBatch;
import org.freeplane.features.icon.TagCategoryNode;
import org.freeplane.features.icon.TagCategorySnapshot;
import org.freeplane.features.icon.TagCategorySnapshotBuilder;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagCategoriesTest;
import org.freeplane.features.map.MapModel;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class FreeplaneTagCategoryAccessTest {
    @Test
    public void appliesMixedBatchAndCommitsExactlyOnce() {
        TagCategories initialTagCategories = TagCategoriesTest.tagCategories("Project\n"
            + " Status\n"
            + " Owner\n"
            + "Team\n"
            + " Member\n");
        MapModel mapModel = Mockito.mock(MapModel.class);
        IconRegistry iconRegistry = Mockito.mock(IconRegistry.class);
        MIconController iconController = Mockito.mock(MIconController.class);
        Mockito.when(mapModel.getIconRegistry()).thenReturn(iconRegistry);
        Mockito.when(iconRegistry.getTagCategories()).thenReturn(initialTagCategories);
        FreeplaneTagCategoryAccess uut = new FreeplaneTagCategoryAccess(iconController);
        String expectedRevision = TagCategorySnapshotBuilder.from(initialTagCategories).getRevision();
        TagCategoryEditBatch editBatch = new TagCategoryEditBatch(expectedRevision, Arrays.asList(
            TagCategoryEdit.add(Arrays.asList("Project", "Priority")),
            TagCategoryEdit.rename(Arrays.asList("Project", "Owner"), "Lead"),
            TagCategoryEdit.move(Arrays.asList("Team", "Member"), Collections.singletonList("Project"), 1),
            TagCategoryEdit.setColor(Arrays.asList("Project", "Member"), "#11223344"),
            TagCategoryEdit.delete(Arrays.asList("Project", "Status")),
            TagCategoryEdit.setSeparator("/")));

        TagCategorySnapshot responseSnapshot = uut.applyEdits(mapModel, editBatch);

        ArgumentCaptor<TagCategories> committedCategoriesCaptor = ArgumentCaptor.forClass(TagCategories.class);
        verify(iconController).setTagCategories(eq(mapModel), committedCategoriesCaptor.capture());
        TagCategories committedTagCategories = committedCategoriesCaptor.getValue();
        assertThat(committedTagCategories.getTagCategorySeparator()).isEqualTo("/");
        assertThat(collectQualifiedNames(responseSnapshot))
            .containsExactly("Project", "Project/Member", "Project/Lead", "Project/Priority", "Team");
        assertThat(collectQualifiedNames(TagCategorySnapshotBuilder.from(committedTagCategories)))
            .containsExactly("Project", "Project/Member", "Project/Lead", "Project/Priority", "Team");
    }

    @Test
    public void staleRevisionFailsWithoutCommit() {
        TagCategories initialTagCategories = TagCategoriesTest.tagCategories("Project\n"
            + " Status\n");
        MapModel mapModel = Mockito.mock(MapModel.class);
        IconRegistry iconRegistry = Mockito.mock(IconRegistry.class);
        MIconController iconController = Mockito.mock(MIconController.class);
        Mockito.when(mapModel.getIconRegistry()).thenReturn(iconRegistry);
        Mockito.when(iconRegistry.getTagCategories()).thenReturn(initialTagCategories);
        FreeplaneTagCategoryAccess uut = new FreeplaneTagCategoryAccess(iconController);
        TagCategoryEditBatch editBatch = new TagCategoryEditBatch("stale-revision",
            Collections.singletonList(TagCategoryEdit.rename(Arrays.asList("Project", "Status"), "State")));

        assertThatThrownBy(() -> uut.applyEdits(mapModel, editBatch))
            .isInstanceOf(TagCategoryConflictException.class)
            .hasMessageContaining("stale revision");
        verify(iconController, never()).setTagCategories(any(), any());
    }

    @Test
    public void failingBatchIsAtomicAndDoesNotCommit() {
        TagCategories initialTagCategories = TagCategoriesTest.tagCategories("Project\n"
            + " Status\n");
        String serializedBefore = initialTagCategories.serialize();
        MapModel mapModel = Mockito.mock(MapModel.class);
        IconRegistry iconRegistry = Mockito.mock(IconRegistry.class);
        MIconController iconController = Mockito.mock(MIconController.class);
        Mockito.when(mapModel.getIconRegistry()).thenReturn(iconRegistry);
        Mockito.when(iconRegistry.getTagCategories()).thenReturn(initialTagCategories);
        FreeplaneTagCategoryAccess uut = new FreeplaneTagCategoryAccess(iconController);
        String expectedRevision = TagCategorySnapshotBuilder.from(initialTagCategories).getRevision();
        TagCategoryEditBatch editBatch = new TagCategoryEditBatch(expectedRevision, Arrays.asList(
            TagCategoryEdit.rename(Arrays.asList("Project", "Status"), "State"),
            TagCategoryEdit.rename(Arrays.asList("Project", "Unknown"), "Renamed")));

        assertThatThrownBy(() -> uut.applyEdits(mapModel, editBatch))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown tag category path");
        verify(iconController, never()).setTagCategories(any(), any());
        assertThat(initialTagCategories.serialize()).isEqualTo(serializedBefore);
    }

    private List<String> collectQualifiedNames(TagCategorySnapshot snapshot) {
        ArrayList<String> qualifiedNames = new ArrayList<>();
        for (TagCategoryNode category : snapshot.getCategories()) {
            collectQualifiedNames(category, qualifiedNames);
        }
        return qualifiedNames;
    }

    private void collectQualifiedNames(TagCategoryNode category, List<String> qualifiedNames) {
        qualifiedNames.add(category.getQualifiedName());
        for (TagCategoryNode child : category.getChildren()) {
            collectQualifiedNames(child, qualifiedNames);
        }
    }
}
