/*
 * Created on 4 May 2024
 *
 * author dimitry
 */
package org.freeplane.features.icon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.freeplane.core.io.IAttributeHandler;
import org.freeplane.core.io.ITreeWriter;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.WriteManager;
import org.freeplane.core.util.ColorUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeBuilder;
import org.freeplane.features.map.NodeModel;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TagCategoriesTest {
    public static TagCategories tagCategories(String input) {
        TagCategories tagCategories = new TagCategories(
                new DefaultMutableTreeNode("tags"),
                new DefaultMutableTreeNode("uncategorized_tags"), "::");
        tagCategories.load(input);
        return tagCategories;
    }

    @Test
    public void testRegisterTagReferenceCategories() {
        TagCategories tagCategories = tagCategories("tag1\n"
                + " tag2\n"
                + "  tag3\n"
                + "tag4\n");

        final DefaultMutableTreeNode rootNode = tagCategories.getRootNode();
        assertThat(rootNode.getChildCount()).isEqualTo(3);

        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) rootNode.getChildAt(0);
        assertThat((tagCategories.categorizedTag(firstChild).getContent())).isEqualTo("tag1");

        DefaultMutableTreeNode secondChild = (DefaultMutableTreeNode) rootNode.getChildAt(1);
        assertThat((tagCategories.categorizedTag(secondChild).getContent())).isEqualTo("tag4");

        DefaultMutableTreeNode grandChild = (DefaultMutableTreeNode) firstChild.getChildAt(0);
        assertThat((tagCategories.categorizedTag(grandChild).getContent())).isEqualTo("tag1::tag2");

        DefaultMutableTreeNode greatGrandChild = (DefaultMutableTreeNode) grandChild.getChildAt(0);
        assertThat((tagCategories.categorizedTag(greatGrandChild).getContent())).isEqualTo("tag1::tag2::tag3");

        TagAssertions.assertThatSerializedWithoutColors(tagCategories)
        .isEqualTo("tag1\n"
                + " tag2\n"
                + "  tag3\n"
                + "tag4\n");
    }

    @Test
    public void modifiesColor() {
        TagCategories uut = new TagCategories(
                new DefaultMutableTreeNode("tags"),
                new DefaultMutableTreeNode("uncategorized_tags"), "::");
        uut.load("AA#11223344\n"
                + " BB#22334455\n");
        uut.setTagColor("AA::BB", Color.BLACK);
        TagAssertions.assertThatSerialized(uut).isEqualTo("AA#11223344\n"
                + " BB#000000ff\n");
        assertThat(uut.getTag(new Tag("AA::BB")).get().getColor())
        .isEqualTo(Color.BLACK);
    }


    @Test
    public void modifiesColorOfTagExistingBeforeLoading() {
        TagCategories uut = new TagCategories(
                new DefaultMutableTreeNode("tags"),
                new DefaultMutableTreeNode("uncategorized_tags"), "::");
        uut.createTag("AA::BB");
        uut.load("AA#11223344\n"
                + " BB#22334455\n");
        uut.setTagColor("AA::BB", Color.BLACK);
        TagAssertions.assertThatSerialized(uut).isEqualTo("AA#11223344\n"
                + " BB#000000ff\n");
        assertThat(uut.getTag(new Tag("AA::BB")).get().getColor())
        .isEqualTo(Color.BLACK);
    }

    @Test
    public void migratesUncategorizedTagsContainingNewSeparator() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"), "::");
        uut.setTagColor("alpha/beta", Color.RED);

        assertThat(tagSpecsWithColors(uut.getUncategorizedTags()))
            .containsExactly("alpha/beta" + ColorUtils.colorToRGBAString(Color.RED));

        uut.updateTagCategorySeparator("/");

        TagAssertions.assertThatSerializedWithoutColors(uut)
            .isEqualTo("alpha\n"
                + " beta\n");
        assertThat(uut.getUncategorizedTags()).isEmpty();
        assertThat(uut.getTagsAsListModel()).map(Tag::getContent)
            .containsExactly("alpha", "alpha/beta");
    }

    @Test
    public void mergesCollidingTagsAfterSeparatorChange() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"), "::");
        uut.setTagColor("team::status", Color.BLUE);
        uut.setTagColor("team/status", Color.RED);

        uut.updateTagCategorySeparator("/");

        TagAssertions.assertThatSerializedWithoutColors(uut)
            .isEqualTo("team\n"
                + " status\n");
        assertThat(uut.getUncategorizedTags()).isEmpty();
        assertThat(uut.getTagsAsListModel()).map(Tag::getContent)
            .containsExactly("team", "team/status");
    }

    @Test
    public void persistsMapTagCategoryDataInRoundtrip() {
        TagCategories source = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"), "::");
        source.load("Project#11223344\n"
            + " Status#22334455\n");
        source.setTagColor("urgent", Color.RED);
        source.setTagColor("review", Color.BLUE);

        String serializedCategories = source.serialize();
        String separator = source.getTagCategorySeparator();
        List<String> uncategorizedTagSpecs = tagSpecsWithColors(source.getUncategorizedTags());

        TagCategories restored = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"), separator);
        restored.setTagCategorySeparator(separator);
        restored.load(serializedCategories);
        source.getUncategorizedTags().forEach(tag -> restored.setTagColor(tag.getContent(), tag.getColor()));

        assertThat(restored.serialize()).isEqualTo(source.serialize());
        assertThat(tagSpecsWithColors(restored.getUncategorizedTags()))
            .containsExactlyElementsOf(uncategorizedTagSpecs);
    }

    @Test
    public void normalizesLoadedTagSegmentsViaCallback() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        TagCategoryRepairService repairService = new TagCategoryRepairService();

        uut.load("ttag#a9d100ff\n"
            + "   #65bdffff\n"
            + "  kk#ceaf00ff\n", repairService::replaceBoundaryWhitespace);

        TagAssertions.assertThatSerializedWithoutColors(uut).isEqualTo("ttag\n"
            + " __\n"
            + "  kk\n");
        assertThat(repairService.repairResult().toMessage())
            .contains("\"  \" -> \"__\"");
    }

    @Test
    public void normalizesQualifiedTagContentBySegments() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        TagCategoryRepairService repairService = new TagCategoryRepairService();

        String normalized = uut.normalizeQualifiedTagContent(
            " Project :: :: State ",
            repairService::replaceBoundaryWhitespace);

        assertThat(normalized).isEqualTo("_Project_::_::_State_");
    }

    @Test
    public void preservesWhitespaceOnlyCategorizedContentBeforeRepair() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");

        uut.load("ttag#a9d100ff\n"
            + "   #65bdffff\n"
            + "  kk#ceaf00ff\n");

        DefaultMutableTreeNode topLevelNode = (DefaultMutableTreeNode) uut.getRootNode().getChildAt(0);
        DefaultMutableTreeNode blankNode = (DefaultMutableTreeNode) topLevelNode.getChildAt(0);
        assertThat(uut.tagWithoutCategories(blankNode).getContent()).isEqualTo("  ");
        assertThat(uut.tagWithoutCategories(blankNode).getColor())
            .isEqualTo(ColorUtils.stringToColor("#65bdffff"));
    }

    @Test
    public void updatesReferencedTagsWithoutNodeTraversal() {
        TagCategories uut = tagCategories("AA#11223344\n"
            + " BB#22334455\n");
        TagReference reference = uut.createTagReference("AA::BB");
        DefaultMutableTreeNode renamedCategoryNode = (DefaultMutableTreeNode) uut.getRootNode().getChildAt(0);
        TreePath renamedPath = new TreePath(uut.getNodes().getPathToRoot(renamedCategoryNode));
        uut.getNodes().valueForPathChanged(renamedPath, new Tag("CC", Color.BLACK));

        uut.replaceReferencedTags(Arrays.asList("AA", "CC"));
        uut.updateTagReferences();

        assertThat(reference.getContent()).isEqualTo("CC::BB");
    }

    @Test
    public void roundtripsNodeTagsUsingTagBuilderAttributeHandlers() {
        MapModel mapModel = new MapModel(
            (source, targetMap, withChildren) -> null,
            new IconRegistry(new TagCategories(
                new DefaultMutableTreeNode("tags"),
                new DefaultMutableTreeNode("uncategorized_tags"),
                "/")),
            null);
        NodeModel sourceNode = new NodeModel("source", mapModel);
        TagCategories tagCategories = mapModel.getIconRegistry().getTagCategories();
        Tags.setTagReferences(sourceNode, Arrays.asList(
            tagCategories.createTagReference("alpha"),
            tagCategories.createTagReference("project/status")));
        TagBuilder uut = new TagBuilder();
        ITreeWriter writer = mock(ITreeWriter.class);

        uut.writeAttributes(writer, sourceNode, sourceNode.getExtension(Tags.class));

        ArgumentCaptor<String> serializedTagsCaptor = ArgumentCaptor.forClass(String.class);
        verify(writer).addAttribute(eq("TAGS"), serializedTagsCaptor.capture());
        String serializedTags = serializedTagsCaptor.getValue();
        ReadManager readManager = new ReadManager();
        WriteManager writeManager = new WriteManager();
        uut.registerBy(readManager, writeManager);
        IAttributeHandler tagsHandler = readManager.getAttributeHandlers()
            .get(NodeBuilder.XML_NODE)
            .get("TAGS");
        NodeModel restoredNode = new NodeModel("restored", mapModel);

        tagsHandler.setAttribute(restoredNode, serializedTags);

        assertThat(Tags.getTagReferences(restoredNode))
            .extracting(TagReference::getContent)
            .containsExactly("alpha", "project/status");
    }

    @Test
    public void returnsOnlyExistingTagReferencesWithoutMutatingRawStorage() {
        MapModel mapModel = new MapModel(
            (source, targetMap, withChildren) -> null,
            new IconRegistry(new TagCategories(
                new DefaultMutableTreeNode("tags"),
                new DefaultMutableTreeNode("uncategorized_tags"),
                "/")),
            null);
        NodeModel node = new NodeModel("node", mapModel);
        TagCategories tagCategories = mapModel.getIconRegistry().getTagCategories();
        Tags.setTagReferences(node, Arrays.asList(
            new TagReference(Tag.REMOVED_TAG),
            tagCategories.createTagReference("project/status")));

        assertThat(Tags.getExistingTagReferences(node))
            .extracting(TagReference::getContent)
            .containsExactly("project/status");
        assertThat(Tags.getTagReferences(node))
            .extracting(TagReference::getContent)
            .containsExactly(" removed tag ", "project/status");
    }

    @Test
    public void omitsRemovedTagsFromUncategorizedTagList() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "/");
        uut.getUncategorizedTagsNode().add(new DefaultMutableTreeNode(Tag.REMOVED_TAG));
        uut.getUncategorizedTagsNode().add(new DefaultMutableTreeNode(new Tag("project")));

        assertThat(uut.getUncategorizedTags())
            .extracting(Tag::getContent)
            .containsExactly("project");
    }

    private static List<String> tagSpecsWithColors(List<Tag> tags) {
        return tags.stream()
            .map(tag -> tag.getContent() + ColorUtils.colorToRGBAString(tag.getColor()))
            .collect(Collectors.toList());
    }
}
