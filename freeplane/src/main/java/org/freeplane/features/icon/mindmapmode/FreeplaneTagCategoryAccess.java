package org.freeplane.features.icon.mindmapmode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagCategoryAccess;
import org.freeplane.features.icon.TagCategoryConflictException;
import org.freeplane.features.icon.TagCategoryEditorDraftSubmission;
import org.freeplane.features.icon.TagCategoryInstruction;
import org.freeplane.features.icon.TagCategoryInstructionRequest;
import org.freeplane.features.icon.TagCategoryInstructionType;
import org.freeplane.features.icon.TagCategoryState;
import org.freeplane.features.icon.TagCategoryStateBuilder;
import org.freeplane.features.icon.TagTargetLocation;
import org.freeplane.features.icon.TagReferenceRewrite;
import org.freeplane.features.map.MapModel;

public class FreeplaneTagCategoryAccess implements TagCategoryAccess {
    private final MIconController iconController;

    public FreeplaneTagCategoryAccess(MIconController iconController) {
        this.iconController = Objects.requireNonNull(iconController, "iconController must not be null");
    }

    @Override
    public TagCategoryState readCurrentCategoryState(MapModel mapModel) {
        return TagCategoryStateBuilder.from(getTagCategories(mapModel));
    }

    @Override
    public TagCategoryState applyInstructionRequest(MapModel mapModel,
                                                    TagCategoryInstructionRequest instructionRequest) {
        Objects.requireNonNull(instructionRequest, "instructionRequest must not be null");
        TagCategories currentTagCategories = getTagCategories(mapModel);
        TagCategoryState currentState = TagCategoryStateBuilder.from(currentTagCategories);
        instructionRequest.requireMatchingRevision(currentState.getRevision());
        TagCategories workingCopy = currentTagCategories.copy();
        for (TagCategoryInstruction instruction : instructionRequest.getInstructions()) {
            applyInstruction(workingCopy, instruction);
        }
        iconController.setTagCategories(mapModel, workingCopy);
        return TagCategoryStateBuilder.from(workingCopy);
    }

    @Override
    public TagCategoryState applyEditorDraftSubmission(MapModel mapModel,
                                                       TagCategoryEditorDraftSubmission draftSubmission) {
        Objects.requireNonNull(draftSubmission, "draftSubmission must not be null");
        TagCategories currentTagCategories = getTagCategories(mapModel);
        TagCategoryState currentState = TagCategoryStateBuilder.from(currentTagCategories);
        draftSubmission.requireMatchingRevision(currentState.getRevision());
        TagCategories draftCategories = draftSubmission.getDraftState().toTagCategories();
        String oldSeparator = draftCategories.getTagCategorySeparator();
        String newSeparator = currentTagCategories.getTagCategorySeparator();
        draftCategories.updateTagCategorySeparator(newSeparator);
        currentTagCategories.getTagsAsListModel().forEach(tag -> draftCategories.registerTagReferenceIfUnknown(tag));
        List<String> replacementPairs = rebaseReplacementPairs(
            TagReferenceRewrite.toPairs(draftSubmission.getReferenceRewrites()),
            oldSeparator,
            newSeparator);
        draftCategories.replaceReferencedTags(replacementPairs);
        iconController.setTagCategories(mapModel, draftCategories);
        return TagCategoryStateBuilder.from(draftCategories);
    }

    private List<String> rebaseReplacementPairs(List<String> replacementPairs,
                                                String oldSeparator,
                                                String newSeparator) {
        if (oldSeparator.equals(newSeparator)) {
            return replacementPairs;
        }
        ArrayList<String> rebasedReplacementPairs = new ArrayList<>(replacementPairs.size());
        for (String replacementPair : replacementPairs) {
            rebasedReplacementPairs.add(replacementPair.replace(oldSeparator, newSeparator));
        }
        return rebasedReplacementPairs;
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

    private void applyInstruction(TagCategories tagCategories, TagCategoryInstruction instruction) {
        TagCategoryInstructionType type = instruction.getType();
        if (type == TagCategoryInstructionType.ADD_TAG) {
            applyAdd(tagCategories, instruction);
            return;
        }
        if (type == TagCategoryInstructionType.DELETE_TAG) {
            applyDelete(tagCategories, instruction);
            return;
        }
        if (type == TagCategoryInstructionType.RENAME_TAG) {
            applyRename(tagCategories, instruction);
            return;
        }
        if (type == TagCategoryInstructionType.MOVE_TAG) {
            applyMove(tagCategories, instruction);
            return;
        }
        if (type == TagCategoryInstructionType.SET_COLOR) {
            applySetColor(tagCategories, instruction);
            return;
        }
        if (type == TagCategoryInstructionType.SET_CATEGORY_SEPARATOR) {
            tagCategories.updateTagCategorySeparator(instruction.getNewSeparator());
            return;
        }
        throw new IllegalArgumentException("Unsupported instruction type: " + type);
    }

    private void applyAdd(TagCategories tagCategories, TagCategoryInstruction instruction) {
        String qualifiedContent = qualifiedContent(tagCategories, instruction.getPath());
        if (instruction.getTargetLocation() == TagTargetLocation.UNCATEGORIZED) {
            tagCategories.createTagReference(singlePathSegment(instruction.getPath()));
        } else {
            tagCategories.createCategorizedTagReference(qualifiedContent);
        }
        if (instruction.getColor() != null && !instruction.getColor().trim().isEmpty()) {
            tagCategories.setTagColor(qualifiedContent, instruction.getColor());
        }
    }

    private void applyDelete(TagCategories tagCategories, TagCategoryInstruction instruction) {
        DefaultMutableTreeNode node = resolveNode(tagCategories, instruction.getPath());
        String oldQualifiedContent = tagCategories.categorizedContent(node);
        tagCategories.removeNodeFromParent(node);
        tagCategories.replaceReferencedTags(Arrays.asList(oldQualifiedContent, ""));
    }

    private void applyRename(TagCategories tagCategories, TagCategoryInstruction instruction) {
        DefaultMutableTreeNode node = resolveNode(tagCategories, instruction.getPath());
        String oldQualifiedContent = tagCategories.categorizedContent(node);
        Tag existingTag = tagCategories.tagWithoutCategories(node);
        TreePath nodePath = new TreePath(tagCategories.getNodes().getPathToRoot(node));
        tagCategories.getNodes().valueForPathChanged(nodePath, new Tag(instruction.getNewName(), existingTag.getColor()));
        String newQualifiedContent = tagCategories.categorizedContent(node);
        tagCategories.replaceReferencedTags(Arrays.asList(oldQualifiedContent, newQualifiedContent));
    }

    private void applyMove(TagCategories tagCategories, TagCategoryInstruction instruction) {
        DefaultMutableTreeNode movedNode = resolveNode(tagCategories, instruction.getPath());
        String oldQualifiedContent = tagCategories.categorizedContent(movedNode);
        DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) movedNode.getParent();
        int oldIndex = oldParent.getIndex(movedNode);
        if (instruction.getTargetLocation() == TagTargetLocation.UNCATEGORIZED && movedNode.getChildCount() > 0) {
            moveSubtreeIntoUncategorized(tagCategories, movedNode, oldQualifiedContent);
            return;
        }
        DefaultMutableTreeNode newParent = instruction.getTargetLocation() == TagTargetLocation.UNCATEGORIZED
            ? tagCategories.getUncategorizedTagsNode()
            : resolveMoveParent(tagCategories, instruction.getPath(), instruction.getNewParentPath());
        if (newParent == tagCategories.getUncategorizedTagsNode() && movedNode.getChildCount() > 0) {
            moveSubtreeIntoUncategorized(tagCategories, movedNode, oldQualifiedContent);
            return;
        }
        int insertionIndex = insertionIndex(newParent, instruction.getIndex(), tagCategories);
        if (oldParent == newParent && instruction.getIndex() != null && oldIndex < insertionIndex) {
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

    private void moveSubtreeIntoUncategorized(TagCategories tagCategories,
                                              DefaultMutableTreeNode movedNode,
                                              String oldQualifiedContent) {
        List<Tag> flattenedTags = collectSubtreeTags(movedNode, tagCategories);
        tagCategories.removeNodeFromParent(movedNode);
        for (Tag flattenedTag : flattenedTags) {
            tagCategories.registerTagReference(flattenedTag);
        }
        tagCategories.replaceReferencedTags(Arrays.asList(oldQualifiedContent, TagCategories.UNCATEGORIZED_NODE));
    }

    private List<Tag> collectSubtreeTags(DefaultMutableTreeNode node, TagCategories tagCategories) {
        ArrayList<Tag> flattenedTags = new ArrayList<>();
        collectSubtreeTags(node, tagCategories, flattenedTags);
        return flattenedTags;
    }

    private void collectSubtreeTags(DefaultMutableTreeNode node,
                                    TagCategories tagCategories,
                                    List<Tag> flattenedTags) {
        Tag tagWithoutCategories = tagCategories.tagWithoutCategories(node);
        flattenedTags.add(new Tag(tagWithoutCategories.getContent(), tagWithoutCategories.getColor()));
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            if (!tagCategories.containsTag(childNode)) {
                continue;
            }
            collectSubtreeTags(childNode, tagCategories, flattenedTags);
        }
    }

    private void applySetColor(TagCategories tagCategories, TagCategoryInstruction instruction) {
        DefaultMutableTreeNode node = resolveNode(tagCategories, instruction.getPath());
        String qualifiedContent = tagCategories.categorizedContent(node);
        tagCategories.setTagColor(qualifiedContent, instruction.getColor());
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

    private DefaultMutableTreeNode resolveMoveParent(TagCategories tagCategories,
                                                     List<String> movedPath,
                                                     List<String> newParentPath) {
        if (newParentPath == null) {
            throw new IllegalArgumentException("newParentPath must not be null");
        }
        if (newParentPath.isEmpty()) {
            return tagCategories.getRootNode();
        }
        if (isOwnSubtreePath(movedPath, newParentPath)) {
            throw new IllegalArgumentException("Cannot move tag into its own subtree");
        }
        tagCategories.createCategorizedTagReference(qualifiedContent(tagCategories, newParentPath));
        DefaultMutableTreeNode parent = resolveNode(tagCategories, newParentPath);
        if (parent.getParent() == tagCategories.getUncategorizedTagsNode()) {
            throw new IllegalArgumentException("Cannot use uncategorized tag as a parent path");
        }
        return parent;
    }

    private boolean isOwnSubtreePath(List<String> movedPath, List<String> newParentPath) {
        if (movedPath == null || newParentPath == null || newParentPath.size() < movedPath.size()) {
            return false;
        }
        for (int i = 0; i < movedPath.size(); i++) {
            if (!movedPath.get(i).equals(newParentPath.get(i))) {
                return false;
            }
        }
        return true;
    }

    private String qualifiedContent(TagCategories tagCategories, List<String> path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
        return path.stream().collect(Collectors.joining(tagCategories.getTagCategorySeparator()));
    }

    private String singlePathSegment(List<String> path) {
        if (path == null || path.size() != 1) {
            throw new IllegalArgumentException("uncategorized tag path must contain exactly one segment");
        }
        return path.get(0);
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

    private DefaultMutableTreeNode findChildBySegment(TagCategories tagCategories,
                                                      DefaultMutableTreeNode node,
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
