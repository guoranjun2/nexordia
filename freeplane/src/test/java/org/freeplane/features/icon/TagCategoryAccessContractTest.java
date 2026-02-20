package org.freeplane.features.icon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.tree.DefaultMutableTreeNode;

import org.junit.Test;

public class TagCategoryAccessContractTest {
    @Test
    public void rejectsMissingRequiredSnapshotFields() {
        assertThatThrownBy(() -> new TagCategorySnapshot(null, "::", Collections.emptyList(), Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("revision");

        assertThatThrownBy(() -> new TagCategorySnapshot("rev", "", Collections.emptyList(), Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("separator");
    }

    @Test
    public void rejectsMissingRequiredEditFields() {
        assertThatThrownBy(() -> new TagCategoryEditBatch(null, Collections.singletonList(TagCategoryEdit.setSeparator("/"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expectedRevision");

        assertThatThrownBy(() -> new TagCategoryEditBatch("rev", Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("operations");

        assertThatThrownBy(() -> TagCategoryEdit.rename(Collections.emptyList(), "Renamed"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("path");

        assertThatThrownBy(() -> TagCategoryEdit.rename(Arrays.asList("Project", "Status"), ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("newName");
    }

    @Test
    public void snapshotOrderingIsDeterministicForEquivalentState() {
        TagCategories firstCategories = TagCategoriesTest.tagCategories("Project\n"
            + " Status\n");
        firstCategories.setTagColor("urgent", Color.RED);

        TagCategories secondCategories = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        secondCategories.createTagReference("urgent");
        secondCategories.createTagReference("Project::Status");
        secondCategories.createTagReference("Project");
        secondCategories.setTagColor("urgent", Color.RED);

        TagCategorySnapshot firstSnapshot = TagCategorySnapshotBuilder.from(firstCategories);
        TagCategorySnapshot secondSnapshot = TagCategorySnapshotBuilder.from(secondCategories);

        assertThat(firstSnapshot).isEqualTo(secondSnapshot);
        assertThat(firstSnapshot.getRevision()).isEqualTo(secondSnapshot.getRevision());
    }

    @Test
    public void revisionIsStableUntilStateChanges() {
        TagCategories uut = TagCategoriesTest.tagCategories("Project\n"
            + " Status\n");
        TagCategorySnapshot firstSnapshot = TagCategorySnapshotBuilder.from(uut);
        TagCategorySnapshot secondSnapshot = TagCategorySnapshotBuilder.from(uut);

        uut.createTagReference("Project::Owner");

        TagCategorySnapshot changedSnapshot = TagCategorySnapshotBuilder.from(uut);

        assertThat(firstSnapshot.getRevision()).isEqualTo(secondSnapshot.getRevision());
        assertThat(changedSnapshot.getRevision()).isNotEqualTo(firstSnapshot.getRevision());
    }

    @Test
    public void staleExpectedRevisionFailsWithoutMutation() {
        TagCategories uut = TagCategoriesTest.tagCategories("Project\n"
            + " Status\n");
        String serializedBefore = uut.serialize();
        TagCategorySnapshot snapshot = TagCategorySnapshotBuilder.from(uut);
        TagCategoryEditBatch batch = new TagCategoryEditBatch(
            "stale-revision",
            Collections.singletonList(TagCategoryEdit.rename(Arrays.asList("Project", "Status"), "State")));

        assertThatThrownBy(() -> batch.requireMatchingRevision(snapshot.getRevision()))
            .isInstanceOf(TagCategoryConflictException.class)
            .hasMessageContaining("revision");
        assertThat(uut.serialize()).isEqualTo(serializedBefore);
    }
}
