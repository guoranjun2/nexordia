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
    public void rejectsMissingRequiredCategoryStateFields() {
        assertThatThrownBy(() -> new TagCategoryState(null, "::", Collections.emptyList(), Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("revision");

        assertThatThrownBy(() -> new TagCategoryState("rev", "", Collections.emptyList(), Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("categorySeparator");
    }

    @Test
    public void rejectsMissingRequiredInstructionFields() {
        assertThatThrownBy(() -> new TagCategoryInstructionRequest(
            null,
            Collections.singletonList(TagCategoryInstruction.setCategorySeparator("/"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("baseRevision");

        assertThatThrownBy(() -> new TagCategoryInstructionRequest("rev", Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("instructions");

        assertThatThrownBy(() -> new TagCategoryEditorDraftSubmission(
            "",
            TagCategoryDraftState.fromTagCategories(TagCategoriesTest.tagCategories("Project\n")),
            Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expectedRevision");

        assertThatThrownBy(() -> new TagCategoryEditorDraftSubmission("rev", null, Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("draftState");

        assertThatThrownBy(() -> TagReferenceRewrite.fromPairs(Collections.singletonList("only-old")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("pairs");

        assertThatThrownBy(() -> TagCategoryInstruction.renameTag(Collections.emptyList(), "Renamed"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("path");

        assertThatThrownBy(() -> TagCategoryInstruction.renameTag(Arrays.asList("Project", "Status"), ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("newName");

        assertThatThrownBy(() -> TagCategoryInstruction.addTag(Arrays.asList("Project", "Status"),
            TagTargetLocation.UNCATEGORIZED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exactly one segment");

        assertThatThrownBy(() -> TagCategoryInstruction.moveTag(Arrays.asList("Project", "Status"),
            TagTargetLocation.CATEGORIZED, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("newParentPath");
    }

    @Test
    public void preservesLegacyEditorRewritePlaceholders() {
        assertThat(TagReferenceRewrite.toPairs(TagReferenceRewrite.fromPairs(Arrays.asList(
            "",
            "Project::State",
            "Project::Old",
            TagCategories.UNCATEGORIZED_NODE,
            "Project::Temp",
            ""))))
            .containsExactly(
                "",
                "Project::State",
                "Project::Old",
                TagCategories.UNCATEGORIZED_NODE,
                "Project::Temp",
                "");
    }

    @Test
    public void categoryStateOrderingIsDeterministicForEquivalentState() {
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

        TagCategoryState firstState = TagCategoryStateBuilder.from(firstCategories);
        TagCategoryState secondState = TagCategoryStateBuilder.from(secondCategories);

        assertThat(firstState).isEqualTo(secondState);
        assertThat(firstState.getRevision()).isEqualTo(secondState.getRevision());
    }

    @Test
    public void revisionIsStableUntilStateChanges() {
        TagCategories uut = TagCategoriesTest.tagCategories("Project\n"
            + " Status\n");
        TagCategoryState firstState = TagCategoryStateBuilder.from(uut);
        TagCategoryState secondState = TagCategoryStateBuilder.from(uut);

        uut.createTagReference("Project::Owner");

        TagCategoryState changedState = TagCategoryStateBuilder.from(uut);

        assertThat(firstState.getRevision()).isEqualTo(secondState.getRevision());
        assertThat(changedState.getRevision()).isNotEqualTo(firstState.getRevision());
    }

    @Test
    public void staleBaseRevisionFailsWithoutMutation() {
        TagCategories uut = TagCategoriesTest.tagCategories("Project\n"
            + " Status\n");
        String serializedBefore = uut.serialize();
        TagCategoryState state = TagCategoryStateBuilder.from(uut);
        TagCategoryInstructionRequest instructionRequest = new TagCategoryInstructionRequest(
            "stale-revision",
            Collections.singletonList(TagCategoryInstruction.renameTag(Arrays.asList("Project", "Status"), "State")));

        assertThatThrownBy(() -> instructionRequest.requireMatchingRevision(state.getRevision()))
            .isInstanceOf(TagCategoryConflictException.class)
            .hasMessageContaining("revision");
        assertThat(uut.serialize()).isEqualTo(serializedBefore);
    }
}
