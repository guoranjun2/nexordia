package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import javax.swing.tree.DefaultMutableTreeNode;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagReference;
import org.freeplane.features.icon.Tags;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.nodestyle.NodeStyleModel;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.junit.Test;

public class NodeContentApplierTest {
    @Test
    public void apply_outputsContentOnAllNodes() {
        MapModel mapModel = new MapModel(
            (source, targetMap, withChildren) -> null, iconRegistry(), null);
        NodeModel parentNode = new NodeModel("parent", mapModel);
        NodeModel childNode = new NodeModel("child", mapModel);
        parentNode.insert(childNode, 0);

        NamedIcon sampleIcon = new MindIcon("node-icon", "/images/node.svg", "node", 0);
        NodeContentWriteRequest parentContent = new NodeContentWriteRequest(
            "root",
            null,
            "details",
            null,
            "note",
            null,
            Collections.singletonList(new AttributeEntry("key", "value")),
            Collections.singletonList("tag"),
            Collections.singletonList(sampleIcon.getName()));
        NodeContentWriteRequest childContent = new NodeContentWriteRequest(
            "child-text",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
        NodeCreationItem childItem = new NodeCreationItem(childContent, Collections.emptyList());
        NodeCreationItem parentItem = new NodeCreationItem(parentContent, Collections.singletonList(childItem));

        IconDescriptionResolver resolver = new IconDescriptionResolver(new DefaultEnglishTextProvider());
        NodeContentApplier uut = new NodeContentApplier(
            new TextualContentEditor(
                mock(TextContentWriteController.class), mock(NoteContentWriteController.class)),
            new AttributesContentEditor(mock(MAttributeController.class)),
            new TagsContentEditor(mock(MIconController.class)),
            new IconsContentEditor(resolver, Collections.singletonList(sampleIcon), mock(MIconController.class)));

        uut.apply(parentNode, parentItem);

        assertThat(parentNode.getText()).isEqualTo("root");
        assertThat(HtmlUtils.htmlToPlain(DetailModel.getDetailText(parentNode))).isEqualTo("details");
        assertThat(HtmlUtils.htmlToPlain(NoteModel.getNoteText(parentNode))).isEqualTo("note");
        NodeAttributeTableModel parentAttributesModel = NodeAttributeTableModel.getModel(parentNode);
        assertThat(parentAttributesModel.getRowCount()).isEqualTo(1);
        assertThat(parentAttributesModel.getName(0)).isEqualTo("key");
        assertThat(parentAttributesModel.getValue(0)).isEqualTo("value");
        assertThat(Tags.getTagReferences(parentNode)).hasSize(1);
        TagReference tagReference = Tags.getTagReferences(parentNode).get(0);
        assertThat(tagReference.getContent()).isEqualTo("tag");
        assertThat(parentNode.getIcons()).hasSize(1);
        assertThat(parentNode.getIcons().get(0)).isSameAs(sampleIcon);
        assertThat(childNode.getText()).isEqualTo("child-text");
    }

    @Test
    public void apply_setsContentTypesWhenProvided() {
        MapModel mapModel = new MapModel(
            (source, targetMap, withChildren) -> null, iconRegistry(), null);
        NodeModel node = new NodeModel("root", mapModel);

        NodeContentWriteRequest content = new NodeContentWriteRequest(
            "Title",
            ContentType.MARKDOWN,
            "x^2",
            ContentType.LATEX,
            "note *value*",
            ContentType.MARKDOWN,
            null,
            null,
            null);
        NodeCreationItem item = new NodeCreationItem(content, Collections.emptyList());

        IconDescriptionResolver resolver = new IconDescriptionResolver(new DefaultEnglishTextProvider());
        NodeContentApplier applier = new NodeContentApplier(
            new TextualContentEditor(
                mock(TextContentWriteController.class), mock(NoteContentWriteController.class)),
            new AttributesContentEditor(mock(MAttributeController.class)),
            new TagsContentEditor(mock(MIconController.class)),
            new IconsContentEditor(resolver, Collections.emptyList(), mock(MIconController.class)));

        applier.apply(node, item);

        assertThat(NodeStyleModel.getNodeFormat(node)).isEqualTo("markdown");
        assertThat(DetailModel.getDetailContentType(node)).isEqualTo("latex");
        assertThat(NoteModel.getNoteContentType(node)).isEqualTo("markdown");
        assertThat(node.getText()).isEqualTo("Title");
        assertThat(DetailModel.getDetailText(node)).isEqualTo("x^2");
        assertThat(NoteModel.getNoteText(node)).isEqualTo("note *value*");
    }

    private static IconRegistry iconRegistry() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("tags");
        DefaultMutableTreeNode uncategorized = new DefaultMutableTreeNode("uncategorized");
        return new IconRegistry(new TagCategories(root, uncategorized, "/"));
    }
}
