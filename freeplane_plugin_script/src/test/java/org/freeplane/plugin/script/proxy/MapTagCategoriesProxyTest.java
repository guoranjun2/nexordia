package org.freeplane.plugin.script.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.freeplane.api.MapTagCategoryInstruction;
import org.freeplane.api.MapTagCategoryInstructionRequest;
import org.freeplane.api.MapTagCategoryInstructionType;
import org.freeplane.api.MapTagCategoryNode;
import org.freeplane.api.MapTagCategoryState;
import org.freeplane.api.MapTagTargetLocation;
import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.TagCategoryAccess;
import org.freeplane.features.icon.TagCategoryConflictException;
import org.freeplane.features.icon.TagCategoryInstruction;
import org.freeplane.features.icon.TagCategoryInstructionRequest;
import org.freeplane.features.icon.TagCategoryInstructionType;
import org.freeplane.features.icon.TagCategoryNode;
import org.freeplane.features.icon.TagCategoryState;
import org.freeplane.features.icon.TagCategoryStateBuilder;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagTargetLocation;
import org.freeplane.features.icon.TagItem;
import org.freeplane.features.icon.mindmapmode.FreeplaneTagCategoryAccess;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.map.MapModel;
import org.freeplane.plugin.script.ScriptContext;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class MapTagCategoriesProxyTest {
    @Test
    public void readReturnsDeterministicState() {
        MapModel mapModel = mock(MapModel.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
        TagCategoryState categoryState = new TagCategoryState(
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
            Collections.singletonList(new TagItem(
                Collections.singletonList("urgent"),
                "urgent",
                "urgent",
                "#ff0000ff")));
        when(tagCategoryAccess.readCurrentCategoryState(mapModel)).thenReturn(categoryState);
        MapTagCategoriesProxy uut = new MapTagCategoriesProxy(mapModel, new ScriptContext(null), tagCategoryAccess);

        MapTagCategoryState firstState = uut.read();
        MapTagCategoryState secondState = uut.read();

        assertThat(firstState).isEqualTo(secondState);
        assertThat(firstState.getRevision()).isEqualTo("sha256:test");
        assertThat(collectQualifiedNames(firstState)).containsExactly("Project", "Project::Status");
        verify(tagCategoryAccess, times(2)).readCurrentCategoryState(mapModel);
    }

    @Test
    public void editConvertsOrderedInstructionsAndReturnsState() {
        MapModel mapModel = mock(MapModel.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
        TagCategoryState appliedState = new TagCategoryState(
            "sha256:applied",
            "/",
            Collections.singletonList(new TagCategoryNode(
                Collections.singletonList("Project"),
                "Project",
                "Project",
                "#11223344",
                Collections.emptyList())),
            Collections.emptyList());
        when(tagCategoryAccess.applyInstructionRequest(eq(mapModel), any())).thenReturn(appliedState);
        MapTagCategoriesProxy uut = new MapTagCategoriesProxy(mapModel, new ScriptContext(null), tagCategoryAccess);
        MapTagCategoryInstruction addInstruction = new MapTagCategoryInstruction(
            MapTagCategoryInstructionType.ADD_TAG,
            Arrays.asList("Project", "Owner"),
            null,
            null,
            MapTagTargetLocation.CATEGORIZED,
            null,
            null,
            null);
        MapTagCategoryInstruction separatorInstruction = new MapTagCategoryInstruction(
            MapTagCategoryInstructionType.SET_CATEGORY_SEPARATOR,
            null,
            null,
            null,
            null,
            null,
            null,
            "/");
        MapTagCategoryInstructionRequest instructionRequest = new MapTagCategoryInstructionRequest(
            "sha256:expected",
            Arrays.asList(addInstruction, separatorInstruction));

        MapTagCategoryState result = uut.edit(instructionRequest);

        ArgumentCaptor<TagCategoryInstructionRequest> requestCaptor = ArgumentCaptor.forClass(TagCategoryInstructionRequest.class);
        verify(tagCategoryAccess).applyInstructionRequest(eq(mapModel), requestCaptor.capture());
        TagCategoryInstructionRequest submittedRequest = requestCaptor.getValue();
        assertThat(submittedRequest.getBaseRevision()).isEqualTo("sha256:expected");
        assertThat(submittedRequest.getInstructions()).extracting(TagCategoryInstruction::getType)
            .containsExactly(TagCategoryInstructionType.ADD_TAG, TagCategoryInstructionType.SET_CATEGORY_SEPARATOR);
        assertThat(submittedRequest.getInstructions().get(0).getPath()).containsExactly("Project", "Owner");
        assertThat(submittedRequest.getInstructions().get(0).getTargetLocation()).isEqualTo(TagTargetLocation.CATEGORIZED);
        assertThat(submittedRequest.getInstructions().get(1).getNewSeparator()).isEqualTo("/");
        assertThat(result.getRevision()).isEqualTo("sha256:applied");
        assertThat(result.getCategorySeparator()).isEqualTo("/");
    }

    @Test
    public void editPropagatesStaleRevisionConflict() {
        MapModel mapModel = mock(MapModel.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
        when(tagCategoryAccess.applyInstructionRequest(eq(mapModel), any()))
            .thenThrow(new TagCategoryConflictException("stale revision"));
        MapTagCategoriesProxy uut = new MapTagCategoriesProxy(mapModel, new ScriptContext(null), tagCategoryAccess);
        MapTagCategoryInstruction separatorInstruction = new MapTagCategoryInstruction(
            MapTagCategoryInstructionType.SET_CATEGORY_SEPARATOR,
            null,
            null,
            null,
            null,
            null,
            null,
            "/");
        MapTagCategoryInstructionRequest instructionRequest = new MapTagCategoryInstructionRequest(
            "sha256:expected",
            Collections.singletonList(separatorInstruction));

        assertThatThrownBy(() -> uut.edit(instructionRequest))
            .isInstanceOf(TagCategoryConflictException.class)
            .hasMessageContaining("stale revision");
    }

    @Test
    public void editResultMatchesCoreServiceOutcomeForEquivalentRequest() {
        TagCategories tagCategories = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        tagCategories.load("Project\n"
            + " Status\n");
        MapModel mapModel = new MapModel(
            (source, targetMap, withChildren) -> null,
            new IconRegistry(tagCategories),
            null);
        MIconController iconController = mock(MIconController.class);
        FreeplaneTagCategoryAccess access = new FreeplaneTagCategoryAccess(iconController);
        MapTagCategoriesProxy uut = new MapTagCategoriesProxy(mapModel, new ScriptContext(null), access);
        String expectedRevision = TagCategoryStateBuilder.from(tagCategories).getRevision();
        TagCategoryInstructionRequest directRequest = new TagCategoryInstructionRequest(
            expectedRevision,
            Arrays.asList(
                TagCategoryInstruction.renameTag(Arrays.asList("Project", "Status"), "State"),
                TagCategoryInstruction.setCategorySeparator("/")));
        TagCategoryState expectedState = access.applyInstructionRequest(mapModel, directRequest);
        MapTagCategoryInstruction renameInstruction = new MapTagCategoryInstruction(
            MapTagCategoryInstructionType.RENAME_TAG,
            Arrays.asList("Project", "Status"),
            "State",
            null,
            null,
            null,
            null,
            null);
        MapTagCategoryInstruction separatorInstruction = new MapTagCategoryInstruction(
            MapTagCategoryInstructionType.SET_CATEGORY_SEPARATOR,
            null,
            null,
            null,
            null,
            null,
            null,
            "/");
        MapTagCategoryInstructionRequest request = new MapTagCategoryInstructionRequest(
            expectedRevision,
            Arrays.asList(renameInstruction, separatorInstruction));

        MapTagCategoryState proxyResult = uut.edit(request);

        assertThat(proxyResult.getRevision()).isEqualTo(expectedState.getRevision());
        assertThat(proxyResult.getCategorySeparator()).isEqualTo(expectedState.getCategorySeparator());
        assertThat(collectQualifiedNames(proxyResult))
            .containsExactlyElementsOf(collectQualifiedNames(expectedState));
    }

    private List<String> collectQualifiedNames(TagCategoryState categoryState) {
        ArrayList<String> qualifiedNames = new ArrayList<>();
        for (TagCategoryNode categoryNode : categoryState.getCategories()) {
            collectQualifiedNames(categoryNode, qualifiedNames);
        }
        return qualifiedNames;
    }

    private List<String> collectQualifiedNames(MapTagCategoryState categoryState) {
        ArrayList<String> qualifiedNames = new ArrayList<>();
        for (MapTagCategoryNode categoryNode : categoryState.getCategories()) {
            collectQualifiedNames(categoryNode, qualifiedNames);
        }
        return qualifiedNames;
    }

    private void collectQualifiedNames(MapTagCategoryNode categoryNode, List<String> qualifiedNames) {
        qualifiedNames.add(categoryNode.getQualifiedName());
        for (MapTagCategoryNode child : categoryNode.getChildren()) {
            collectQualifiedNames(child, qualifiedNames);
        }
    }

    private void collectQualifiedNames(TagCategoryNode categoryNode, List<String> qualifiedNames) {
        qualifiedNames.add(categoryNode.getQualifiedName());
        for (TagCategoryNode child : categoryNode.getChildren()) {
            collectQualifiedNames(child, qualifiedNames);
        }
    }
}
