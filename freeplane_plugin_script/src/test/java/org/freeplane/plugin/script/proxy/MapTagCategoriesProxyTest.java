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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.TagCategoryAccess;
import org.freeplane.features.icon.TagCategoryConflictException;
import org.freeplane.features.icon.TagCategoryEdit;
import org.freeplane.features.icon.TagCategoryEditBatch;
import org.freeplane.features.icon.TagCategoryEditType;
import org.freeplane.features.icon.TagCategoryNode;
import org.freeplane.features.icon.TagCategorySnapshot;
import org.freeplane.features.icon.TagCategorySnapshotBuilder;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagDescriptor;
import org.freeplane.features.icon.mindmapmode.FreeplaneTagCategoryAccess;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.map.MapModel;
import org.freeplane.plugin.script.ScriptContext;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class MapTagCategoriesProxyTest {
    @Test
    public void snapshotReturnsDeterministicPayload() {
        MapModel mapModel = mock(MapModel.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
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
        MapTagCategoriesProxy uut = new MapTagCategoriesProxy(mapModel, new ScriptContext(null), tagCategoryAccess);

        Map<String, Object> firstSnapshot = uut.snapshot();
        Map<String, Object> secondSnapshot = uut.snapshot();

        assertThat(firstSnapshot).isEqualTo(secondSnapshot);
        assertThat(firstSnapshot.get("revision")).isEqualTo("sha256:test");
        assertThat(collectQualifiedNames(firstSnapshot))
            .containsExactly("Project", "Project::Status");
        verify(tagCategoryAccess, times(2)).readSnapshot(mapModel);
    }

    @Test
    public void applyConvertsOrderedOperationsAndReturnsSnapshot() {
        MapModel mapModel = mock(MapModel.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
        TagCategorySnapshot appliedSnapshot = new TagCategorySnapshot(
            "sha256:applied",
            "/",
            Collections.singletonList(new TagCategoryNode(
                Collections.singletonList("Project"),
                "Project",
                "Project",
                "#11223344",
                Collections.emptyList())),
            Collections.emptyList());
        when(tagCategoryAccess.applyEdits(eq(mapModel), any())).thenReturn(appliedSnapshot);
        MapTagCategoriesProxy uut = new MapTagCategoriesProxy(mapModel, new ScriptContext(null), tagCategoryAccess);
        Map<String, Object> renameOperation = new LinkedHashMap<>();
        renameOperation.put("type", "RENAME");
        renameOperation.put("path", Arrays.asList("Project", "Status"));
        renameOperation.put("newName", "State");
        Map<String, Object> separatorOperation = new LinkedHashMap<>();
        separatorOperation.put("type", "SET_SEPARATOR");
        separatorOperation.put("newSeparator", "/");
        Map<String, Object> editBatch = new LinkedHashMap<>();
        editBatch.put("expectedRevision", "sha256:expected");
        editBatch.put("operations", Arrays.asList(renameOperation, separatorOperation));

        Map<String, Object> result = uut.apply(editBatch);

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
        assertThat(result.get("revision")).isEqualTo("sha256:applied");
        assertThat(result.get("separator")).isEqualTo("/");
    }

    @Test
    public void applyPropagatesStaleRevisionConflict() {
        MapModel mapModel = mock(MapModel.class);
        TagCategoryAccess tagCategoryAccess = mock(TagCategoryAccess.class);
        when(tagCategoryAccess.applyEdits(eq(mapModel), any()))
            .thenThrow(new TagCategoryConflictException("stale revision"));
        MapTagCategoriesProxy uut = new MapTagCategoriesProxy(mapModel, new ScriptContext(null), tagCategoryAccess);
        Map<String, Object> separatorOperation = new LinkedHashMap<>();
        separatorOperation.put("type", "SET_SEPARATOR");
        separatorOperation.put("newSeparator", "/");
        Map<String, Object> editBatch = new LinkedHashMap<>();
        editBatch.put("expectedRevision", "sha256:expected");
        editBatch.put("operations", Collections.singletonList(separatorOperation));

        assertThatThrownBy(() -> uut.apply(editBatch))
            .isInstanceOf(TagCategoryConflictException.class)
            .hasMessageContaining("stale revision");
    }

    @Test
    public void applyResultMatchesCoreServiceOutcomeForEquivalentBatch() {
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
        String expectedRevision = TagCategorySnapshotBuilder.from(tagCategories).getRevision();
        TagCategoryEditBatch directBatch = new TagCategoryEditBatch(
            expectedRevision,
            Arrays.asList(
                TagCategoryEdit.rename(Arrays.asList("Project", "Status"), "State"),
                TagCategoryEdit.setSeparator("/")));
        TagCategorySnapshot expectedSnapshot = access.applyEdits(mapModel, directBatch);
        Map<String, Object> renameOperation = new LinkedHashMap<>();
        renameOperation.put("type", "RENAME");
        renameOperation.put("path", Arrays.asList("Project", "Status"));
        renameOperation.put("newName", "State");
        Map<String, Object> separatorOperation = new LinkedHashMap<>();
        separatorOperation.put("type", "SET_SEPARATOR");
        separatorOperation.put("newSeparator", "/");
        Map<String, Object> editBatch = new LinkedHashMap<>();
        editBatch.put("expectedRevision", expectedRevision);
        editBatch.put("operations", Arrays.asList(renameOperation, separatorOperation));

        Map<String, Object> proxyResult = uut.apply(editBatch);

        assertThat(proxyResult.get("revision")).isEqualTo(expectedSnapshot.getRevision());
        assertThat(proxyResult.get("separator")).isEqualTo(expectedSnapshot.getSeparator());
        assertThat(collectQualifiedNames(proxyResult))
            .containsExactlyElementsOf(collectQualifiedNames(expectedSnapshot));
    }

    private List<String> collectQualifiedNames(TagCategorySnapshot snapshot) {
        ArrayList<String> qualifiedNames = new ArrayList<>();
        for (TagCategoryNode categoryNode : snapshot.getCategories()) {
            collectQualifiedNames(categoryNode, qualifiedNames);
        }
        return qualifiedNames;
    }

    @SuppressWarnings("unchecked")
    private List<String> collectQualifiedNames(Map<String, Object> snapshotMap) {
        ArrayList<String> qualifiedNames = new ArrayList<>();
        List<Map<String, Object>> categories = (List<Map<String, Object>>) snapshotMap.get("categories");
        for (Map<String, Object> category : categories) {
            collectQualifiedNames(category, qualifiedNames);
        }
        return qualifiedNames;
    }

    @SuppressWarnings("unchecked")
    private void collectQualifiedNames(Map<String, Object> category, List<String> qualifiedNames) {
        qualifiedNames.add((String) category.get("qualifiedName"));
        List<Map<String, Object>> children = (List<Map<String, Object>>) category.get("children");
        for (Map<String, Object> child : children) {
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
