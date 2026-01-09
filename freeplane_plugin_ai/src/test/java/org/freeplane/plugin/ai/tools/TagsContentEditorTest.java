package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import javax.swing.tree.DefaultMutableTreeNode;

import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagReference;
import org.freeplane.features.icon.Tags;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.junit.Test;

public class TagsContentEditorTest {
    @Test
    public void apply_addsTags() {
        MapModel mapModel = new MapModel(
            (source, targetMap, withChildren) -> null, iconRegistry(), null);
        NodeModel nodeModel = new NodeModel("node", mapModel);
        TagsContent tagsContent = new TagsContent(Collections.singletonList("flag"));
        TagsContentEditor unitUnderTest = new TagsContentEditor();

        unitUnderTest.apply(nodeModel, tagsContent);

        assertThat(Tags.getTagReferences(nodeModel)).hasSize(1);
        TagReference reference = Tags.getTagReferences(nodeModel).get(0);
        assertThat(reference.getContent()).isEqualTo("flag");
    }

    private static IconRegistry iconRegistry() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("tags");
        DefaultMutableTreeNode uncategorized = new DefaultMutableTreeNode("uncategorized");
        return new IconRegistry(new TagCategories(root, uncategorized, "/"));
    }
}
