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

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagCategoriesTest;
import org.freeplane.features.icon.TagCategoryConflictException;
import org.freeplane.features.icon.TagCategoryDraftState;
import org.freeplane.features.icon.TagCategoryEditorDraftSubmission;
import org.freeplane.features.icon.TagCategoryInstruction;
import org.freeplane.features.icon.TagCategoryInstructionRequest;
import org.freeplane.features.icon.TagCategoryNode;
import org.freeplane.features.icon.TagCategoryState;
import org.freeplane.features.icon.TagCategoryStateBuilder;
import org.freeplane.features.icon.TagItem;
import org.freeplane.features.icon.TagReferenceRewrite;
import org.freeplane.features.map.MapModel;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class FreeplaneTagCategoryAccessTest {
    @Test
    public void appliesMixedInstructionRequestAndCommitsExactlyOnce() {
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
        String expectedRevision = TagCategoryStateBuilder.from(initialTagCategories).getRevision();
        TagCategoryInstructionRequest instructionRequest = new TagCategoryInstructionRequest(expectedRevision, Arrays.asList(
            TagCategoryInstruction.addCategory(Arrays.asList("Project", "Priority")),
            TagCategoryInstruction.renameCategory(Arrays.asList("Project", "Owner"), "Lead"),
            TagCategoryInstruction.moveCategory(Arrays.asList("Team", "Member"), Collections.singletonList("Project"), 1),
            TagCategoryInstruction.setColor(Arrays.asList("Project", "Member"), "#11223344"),
            TagCategoryInstruction.deleteCategory(Arrays.asList("Project", "Status")),
            TagCategoryInstruction.setCategorySeparator("/")));

        TagCategoryState responseState = uut.applyInstructionRequest(mapModel, instructionRequest);

        ArgumentCaptor<TagCategories> committedCategoriesCaptor = ArgumentCaptor.forClass(TagCategories.class);
        verify(iconController).setTagCategories(eq(mapModel), committedCategoriesCaptor.capture());
        TagCategories committedTagCategories = committedCategoriesCaptor.getValue();
        assertThat(committedTagCategories.getTagCategorySeparator()).isEqualTo("/");
        assertThat(collectQualifiedNames(responseState))
            .containsExactly("Project", "Project/Member", "Project/Lead", "Project/Priority", "Team");
        assertThat(collectQualifiedNames(TagCategoryStateBuilder.from(committedTagCategories)))
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
        TagCategoryInstructionRequest instructionRequest = new TagCategoryInstructionRequest(
            "stale-revision",
            Collections.singletonList(TagCategoryInstruction.renameCategory(Arrays.asList("Project", "Status"), "State")));

        assertThatThrownBy(() -> uut.applyInstructionRequest(mapModel, instructionRequest))
            .isInstanceOf(TagCategoryConflictException.class)
            .hasMessageContaining("stale revision");
        verify(iconController, never()).setTagCategories(any(), any());
    }

    @Test
    public void failingInstructionRequestIsAtomicAndDoesNotCommit() {
        TagCategories initialTagCategories = TagCategoriesTest.tagCategories("Project\n"
            + " Status\n");
        String serializedBefore = initialTagCategories.serialize();
        MapModel mapModel = Mockito.mock(MapModel.class);
        IconRegistry iconRegistry = Mockito.mock(IconRegistry.class);
        MIconController iconController = Mockito.mock(MIconController.class);
        Mockito.when(mapModel.getIconRegistry()).thenReturn(iconRegistry);
        Mockito.when(iconRegistry.getTagCategories()).thenReturn(initialTagCategories);
        FreeplaneTagCategoryAccess uut = new FreeplaneTagCategoryAccess(iconController);
        String expectedRevision = TagCategoryStateBuilder.from(initialTagCategories).getRevision();
        TagCategoryInstructionRequest instructionRequest = new TagCategoryInstructionRequest(expectedRevision, Arrays.asList(
            TagCategoryInstruction.renameCategory(Arrays.asList("Project", "Status"), "State"),
            TagCategoryInstruction.renameCategory(Arrays.asList("Project", "Unknown"), "Renamed")));

        assertThatThrownBy(() -> uut.applyInstructionRequest(mapModel, instructionRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown tag category path");
        verify(iconController, never()).setTagCategories(any(), any());
        assertThat(initialTagCategories.serialize()).isEqualTo(serializedBefore);
    }

    @Test
    public void applyEditorDraftSubmissionCommitsDraftAndUpdatesReferences() {
        TagCategories initialTagCategories = TagCategoriesTest.tagCategories("AA#11223344\n"
            + " BB#22334455\n");
        MapModel mapModel = Mockito.mock(MapModel.class);
        IconRegistry iconRegistry = Mockito.mock(IconRegistry.class);
        MIconController iconController = Mockito.mock(MIconController.class);
        Mockito.when(mapModel.getIconRegistry()).thenReturn(iconRegistry);
        Mockito.when(iconRegistry.getTagCategories()).thenReturn(initialTagCategories);
        FreeplaneTagCategoryAccess uut = new FreeplaneTagCategoryAccess(iconController);
        TagCategories draftCategories = initialTagCategories.copy();
        DefaultMutableTreeNode categoryNode = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) draftCategories
            .getRootNode()
            .getChildAt(0))
            .getChildAt(0);
        draftCategories.getNodes().valueForPathChanged(
            new TreePath(draftCategories.getNodes().getPathToRoot(categoryNode)),
            new Tag("STATE", draftCategories.tagWithoutCategories(categoryNode).getColor()));
        String expectedRevision = TagCategoryStateBuilder.from(initialTagCategories).getRevision();
        TagCategoryEditorDraftSubmission draftSubmission = new TagCategoryEditorDraftSubmission(
            expectedRevision,
            TagCategoryDraftState.fromTagCategories(draftCategories),
            TagReferenceRewrite.fromPairs(Arrays.asList("AA::BB", "AA::STATE")));

        TagCategoryState responseState = uut.applyEditorDraftSubmission(mapModel, draftSubmission);

        verify(iconController).setTagCategories(eq(mapModel), eq(draftCategories));
        assertThat(collectQualifiedNames(responseState)).containsExactly("AA", "AA::STATE");
        assertThat(draftCategories.getTagsAsListModel()).extracting(tag -> tag.getContent())
            .containsExactly("AA", "AA::STATE");
    }

    @Test
    public void applyEditorDraftSubmissionRejectsStaleRevisionWithoutCommit() {
        TagCategories initialTagCategories = TagCategoriesTest.tagCategories("AA#11223344\n"
            + " BB#22334455\n");
        MapModel mapModel = Mockito.mock(MapModel.class);
        IconRegistry iconRegistry = Mockito.mock(IconRegistry.class);
        MIconController iconController = Mockito.mock(MIconController.class);
        Mockito.when(mapModel.getIconRegistry()).thenReturn(iconRegistry);
        Mockito.when(iconRegistry.getTagCategories()).thenReturn(initialTagCategories);
        FreeplaneTagCategoryAccess uut = new FreeplaneTagCategoryAccess(iconController);
        TagCategories draftCategories = initialTagCategories.copy();
        TagCategoryEditorDraftSubmission draftSubmission = new TagCategoryEditorDraftSubmission(
            "stale-revision",
            TagCategoryDraftState.fromTagCategories(draftCategories),
            TagReferenceRewrite.fromPairs(Arrays.asList("AA::BB", "AA::STATE")));

        assertThatThrownBy(() -> uut.applyEditorDraftSubmission(mapModel, draftSubmission))
            .isInstanceOf(TagCategoryConflictException.class)
            .hasMessageContaining("stale revision");
        verify(iconController, never()).setTagCategories(any(), any());
    }

    @Test
    public void moveSubtreeIntoUncategorizedFlattensMovedPaths() {
        TagCategories initialTagCategories = TagCategoriesTest.tagCategories("AA#11223344\n"
            + " BB#22334455\n"
            + "  CC#33445566\n"
            + "DD#44556677\n");
        initialTagCategories.registerTag("UU");
        initialTagCategories.registerTag("VV");
        MapModel mapModel = Mockito.mock(MapModel.class);
        IconRegistry iconRegistry = Mockito.mock(IconRegistry.class);
        MIconController iconController = Mockito.mock(MIconController.class);
        Mockito.when(mapModel.getIconRegistry()).thenReturn(iconRegistry);
        Mockito.when(iconRegistry.getTagCategories()).thenReturn(initialTagCategories);
        FreeplaneTagCategoryAccess uut = new FreeplaneTagCategoryAccess(iconController);
        String expectedRevision = TagCategoryStateBuilder.from(initialTagCategories).getRevision();
        TagCategoryInstructionRequest instructionRequest = new TagCategoryInstructionRequest(
            expectedRevision,
            Collections.singletonList(
                TagCategoryInstruction.moveCategory(
                    Arrays.asList("AA", "BB"),
                    Collections.singletonList(TagCategories.UNCATEGORIZED_NODE),
                    null)));

        TagCategoryState responseState = uut.applyInstructionRequest(mapModel, instructionRequest);

        ArgumentCaptor<TagCategories> committedCategoriesCaptor = ArgumentCaptor.forClass(TagCategories.class);
        verify(iconController).setTagCategories(eq(mapModel), committedCategoriesCaptor.capture());
        TagCategories committedTagCategories = committedCategoriesCaptor.getValue();
        assertThat(collectQualifiedNames(responseState)).containsExactly("AA", "DD");
        assertThat(responseState.getUncategorizedTags())
            .extracting(TagItem::getQualifiedName)
            .containsExactly("BB", "CC", "UU", "VV");
        assertThat(committedTagCategories.getTagsAsListModel()).extracting(tag -> tag.getContent())
            .containsExactly("AA", "BB", "CC", "DD", "UU", "VV");
    }

    private List<String> collectQualifiedNames(TagCategoryState categoryState) {
        ArrayList<String> qualifiedNames = new ArrayList<>();
        for (TagCategoryNode category : categoryState.getCategories()) {
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
