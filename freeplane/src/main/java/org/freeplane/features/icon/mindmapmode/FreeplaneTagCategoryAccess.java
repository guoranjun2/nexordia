package org.freeplane.features.icon.mindmapmode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagCategoryAccess;
import org.freeplane.features.icon.TagCategoryEdit;
import org.freeplane.features.icon.TagCategoryEditBatch;
import org.freeplane.features.icon.TagCategoryEditType;
import org.freeplane.features.icon.TagCategorySnapshot;
import org.freeplane.features.icon.TagCategorySnapshotBuilder;
import org.freeplane.features.map.MapModel;

public class FreeplaneTagCategoryAccess implements TagCategoryAccess {
    private final MIconController iconController;

    public FreeplaneTagCategoryAccess(MIconController iconController) {
        this.iconController = Objects.requireNonNull(iconController, "iconController must not be null");
    }

    @Override
    public TagCategorySnapshot readSnapshot(MapModel mapModel) {
        return TagCategorySnapshotBuilder.from(getTagCategories(mapModel));
    }

    @Override
    public TagCategorySnapshot applyEdits(MapModel mapModel, TagCategoryEditBatch editBatch) {
        Objects.requireNonNull(editBatch, "editBatch must not be null");
        TagCategories currentTagCategories = getTagCategories(mapModel);
        TagCategorySnapshot currentSnapshot = TagCategorySnapshotBuilder.from(currentTagCategories);
        editBatch.requireMatchingRevision(currentSnapshot.getRevision());
        TagCategories workingCopy = currentTagCategories.copy();
        for (TagCategoryEdit operation : editBatch.getOperations()) {
            applyOperation(workingCopy, operation);
        }
        iconController.setTagCategories(mapModel, workingCopy);
        return TagCategorySnapshotBuilder.from(workingCopy);
    }

    private TagCategories getTagCategories(MapModel mapModel) {
        Objects.requireNonNull(mapModel, "mapModel must not be null");
        if (mapModel.getIconRegistry() == null) {
            throw new IllegalArgumentException("mapModel has no icon registry");
        }
        TagCategories tagCategories = mapModel.getIconRegistry().getTagCategories();
        if (tagCategories == null) {
            throw new IllegalArgumentException("mapModel has no tag categories");
        }
        return tagCategories;
    }

    private void applyOperation(TagCategories tagCategories, TagCategoryEdit operation) {
        TagCategoryEditType type = operation.getType();
        if (type == TagCategoryEditType.ADD) {
            applyAdd(tagCategories, operation);
            return;
        }
        if (type == TagCategoryEditType.DELETE) {
            applyDelete(tagCategories, operation);
            return;
        }
        if (type == TagCategoryEditType.RENAME) {
            applyRename(tagCategories, operation);
            return;
        }
        if (type == TagCategoryEditType.MOVE) {
            applyMove(tagCategories, operation);
            return;
        }
        if (type == TagCategoryEditType.SET_COLOR) {
            applySetColor(tagCategories, operation);
            return;
        }
        if (type == TagCategoryEditType.SET_SEPARATOR) {
            tagCategories.updateTagCategorySeparator(operation.getNewSeparator());
            return;
        }
        throw new IllegalArgumentException("Unsupported edit type: " + type);
    }

    private void applyAdd(TagCategories tagCategories, TagCategoryEdit operation) {
        String qualifiedContent = qualifiedContent(tagCategories, operation.getPath());
        tagCategories.createTagReference(qualifiedContent);
        if (operation.getColor() != null && !operation.getColor().trim().isEmpty()) {
            tagCategories.setTagColor(qualifiedContent, operation.getColor());
        }
    }

    private void applyDelete(TagCategories tagCategories, TagCategoryEdit operation) {
        DefaultMutableTreeNode node = resolveNode(tagCategories, operation.getPath());
        String oldQualifiedContent = tagCategories.categorizedContent(node);
        tagCategories.removeNodeFromParent(node);
        tagCategories.replaceReferencedTags(Arrays.asList(oldQualifiedContent, ""));
    }

    private void applyRename(TagCategories tagCategories, TagCategoryEdit operation) {
        DefaultMutableTreeNode node = resolveNode(tagCategories, operation.getPath());
        String oldQualifiedContent = tagCategories.categorizedContent(node);
        Tag existingTag = tagCategories.tagWithoutCategories(node);
        TreePath nodePath = new TreePath(tagCategories.getNodes().getPathToRoot(node));
        tagCategories.getNodes().valueForPathChanged(nodePath, new Tag(operation.getNewName(), existingTag.getColor()));
        String newQualifiedContent = tagCategories.categorizedContent(node);
        tagCategories.replaceReferencedTags(Arrays.asList(oldQualifiedContent, newQualifiedContent));
    }

    private void applyMove(TagCategories tagCategories, TagCategoryEdit operation) {
        DefaultMutableTreeNode movedNode = resolveNode(tagCategories, operation.getPath());
        String oldQualifiedContent = tagCategories.categorizedContent(movedNode);
        DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) movedNode.getParent();
        int oldIndex = oldParent.getIndex(movedNode);
        DefaultMutableTreeNode newParent = resolveMoveParent(tagCategories, operation.getNewParentPath());
        if (newParent == tagCategories.getUncategorizedTagsNode() && movedNode.getChildCount() > 0) {
            throw new IllegalArgumentException("Cannot move non-leaf category into uncategorized");
        }
        int insertionIndex = insertionIndex(newParent, operation.getIndex(), tagCategories);
        if (oldParent == newParent && operation.getIndex() != null && oldIndex < insertionIndex) {
            insertionIndex--;
        }
        tagCategories.removeNodeFromParent(movedNode);
        insertionIndex = normalizeInsertionIndex(newParent, insertionIndex, tagCategories);
        tagCategories.getNodes().insertNodeInto(movedNode, newParent, insertionIndex);
        DefaultMutableTreeNode keptNode = tagCategories.merge(movedNode);
        DefaultMutableTreeNode resultingNode = keptNode == null ? movedNode : keptNode;
        String newQualifiedContent = tagCategories.categorizedContent(resultingNode);
        tagCategories.replaceReferencedTags(Arrays.asList(oldQualifiedContent, newQualifiedContent));
    }

    private void applySetColor(TagCategories tagCategories, TagCategoryEdit operation) {
        DefaultMutableTreeNode node = resolveNode(tagCategories, operation.getPath());
        String qualifiedContent = tagCategories.categorizedContent(node);
        tagCategories.setTagColor(qualifiedContent, operation.getColor());
        tagCategories.fireNodeChanged(node);
    }

    private int insertionIndex(DefaultMutableTreeNode newParent, Integer requestedIndex, TagCategories tagCategories) {
        int maxInsertionIndex = maxInsertionIndex(newParent, tagCategories);
        if (requestedIndex == null) {
            return maxInsertionIndex;
        }
        if (requestedIndex < 0 || requestedIndex > maxInsertionIndex) {
            throw new IllegalArgumentException("Invalid insertion index: " + requestedIndex);
        }
        return requestedIndex;
    }

    private int normalizeInsertionIndex(DefaultMutableTreeNode parent, int requestedIndex, TagCategories tagCategories) {
        int maxInsertionIndex = maxInsertionIndex(parent, tagCategories);
        if (requestedIndex > maxInsertionIndex) {
            return maxInsertionIndex;
        }
        return requestedIndex;
    }

    private int maxInsertionIndex(DefaultMutableTreeNode parent, TagCategories tagCategories) {
        if (parent == tagCategories.getRootNode()) {
            return parent.getChildCount() - 1;
        }
        return parent.getChildCount();
    }

    private DefaultMutableTreeNode resolveMoveParent(TagCategories tagCategories, List<String> newParentPath) {
        if (newParentPath == null) {
            throw new IllegalArgumentException("newParentPath must not be null");
        }
        if (newParentPath.isEmpty()) {
            return tagCategories.getRootNode();
        }
        if (newParentPath.size() == 1 && TagCategories.UNCATEGORIZED_NODE.equals(newParentPath.get(0))) {
            return tagCategories.getUncategorizedTagsNode();
        }
        DefaultMutableTreeNode parent = resolveNode(tagCategories, newParentPath);
        if (parent.getParent() == tagCategories.getUncategorizedTagsNode()) {
            throw new IllegalArgumentException("Cannot use uncategorized tag as a parent path");
        }
        return parent;
    }

    private String qualifiedContent(TagCategories tagCategories, List<String> path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
        return path.stream().collect(Collectors.joining(tagCategories.getTagCategorySeparator()));
    }

    private DefaultMutableTreeNode resolveNode(TagCategories tagCategories, List<String> path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
        DefaultMutableTreeNode currentNode = tagCategories.getRootNode();
        for (int i = 0; i < path.size(); i++) {
            String segment = path.get(i);
            DefaultMutableTreeNode childNode = findChildBySegment(tagCategories, currentNode, segment);
            if (childNode == null && currentNode == tagCategories.getRootNode() && i == path.size() - 1) {
                childNode = findUncategorizedNodeBySegment(tagCategories, segment);
            }
            if (childNode == null) {
                throw new IllegalArgumentException("Unknown tag category path: " + path);
            }
            currentNode = childNode;
        }
        return currentNode;
    }

    private DefaultMutableTreeNode findChildBySegment(TagCategories tagCategories, DefaultMutableTreeNode node,
                                                      String segment) {
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            if (!tagCategories.containsTag(childNode)) {
                continue;
            }
            Tag tagWithoutCategories = tagCategories.tagWithoutCategories(childNode);
            if (tagWithoutCategories.getContent().equals(segment)) {
                return childNode;
            }
        }
        return null;
    }

    private DefaultMutableTreeNode findUncategorizedNodeBySegment(TagCategories tagCategories, String segment) {
        DefaultMutableTreeNode uncategorizedNode = tagCategories.getUncategorizedTagsNode();
        for (int i = 0; i < uncategorizedNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) uncategorizedNode.getChildAt(i);
            Tag uncategorizedTag = tagCategories.tagWithoutCategories(childNode);
            if (uncategorizedTag.getContent().equals(segment)) {
                return childNode;
            }
        }
        return null;
    }
}
