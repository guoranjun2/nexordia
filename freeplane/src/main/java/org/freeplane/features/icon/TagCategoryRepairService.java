package org.freeplane.features.icon;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;

public class TagCategoryRepairService {
    public static class RepairResult {
        private final List<String> changes;

        RepairResult(List<String> changes) {
            this.changes = changes;
        }

        public boolean hasChanges() {
            return !changes.isEmpty();
        }

        public String toMessage() {
            if (!hasChanges()) {
                return "";
            }
            return translatedText("tag_category_repair_report_prefix", "Legacy tag names were normalized:")
                + System.lineSeparator()
                + String.join(System.lineSeparator(), changes);
        }
    }

    private static class ReferenceReplacement {
        private final String from;
        private final String to;

        ReferenceReplacement(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    public RepairResult repair(TagCategories tagCategories) {
        Objects.requireNonNull(tagCategories, "tagCategories must not be null");
        ArrayList<String> changes = new ArrayList<>();
        ArrayList<ReferenceReplacement> replacements = new ArrayList<>();
        repairCategorizedNodes(tagCategories, tagCategories.getRootNode(), replacements, changes);
        repairUncategorizedNodes(tagCategories, replacements, changes);
        applyReferenceRewrites(tagCategories, replacements);
        return new RepairResult(changes);
    }

    public RepairResult repairLoadedMap(MapModel mapModel) {
        Objects.requireNonNull(mapModel, "mapModel must not be null");
        return repair(mapModel.getIconRegistry().getTagCategories());
    }

    private void repairCategorizedNodes(TagCategories tagCategories,
                                        DefaultMutableTreeNode parentNode,
                                        List<ReferenceReplacement> replacements,
                                        List<String> changes) {
        List<DefaultMutableTreeNode> childNodes = childTagNodes(parentNode, tagCategories);
        for (DefaultMutableTreeNode childNode : childNodes) {
            repairCategorizedNodes(tagCategories, childNode, replacements, changes);
            repairCategorizedNode(tagCategories, childNode, replacements, changes);
        }
    }

    private void repairCategorizedNode(TagCategories tagCategories,
                                       DefaultMutableTreeNode node,
                                       List<ReferenceReplacement> replacements,
                                       List<String> changes) {
        if (node.getParent() == null) {
            return;
        }
        Tag tag = tagCategories.tagWithoutCategories(node);
        String originalQualifiedName = tagCategories.categorizedContent(node);
        String normalizedName = replaceBoundaryWhitespace(tag.getContent());
        String normalizedQualifiedName = replaceBoundaryWhitespaceInQualifiedName(
            originalQualifiedName,
            tagCategories.getTagCategorySeparator());
        if (!originalQualifiedName.equals(normalizedQualifiedName)) {
            replacements.add(new ReferenceReplacement(originalQualifiedName, normalizedQualifiedName));
            changes.add(changeDescription(originalQualifiedName, normalizedQualifiedName));
        }
        if (!normalizedName.equals(tag.getContent())) {
            DefaultTreeModel nodes = tagCategories.getNodes();
            TreePath pathToRoot = new TreePath(nodes.getPathToRoot(node));
            nodes.valueForPathChanged(pathToRoot, new Tag(normalizedName, tag.getColor()));
        }
        tagCategories.merge(node);
    }

    private void repairUncategorizedNodes(TagCategories tagCategories,
                                          List<ReferenceReplacement> replacements,
                                          List<String> changes) {
        DefaultMutableTreeNode uncategorizedNode = tagCategories.getUncategorizedTagsNode();
        List<DefaultMutableTreeNode> childNodes = childTagNodes(uncategorizedNode, tagCategories);
        for (DefaultMutableTreeNode childNode : childNodes) {
            Tag tag = tagCategories.tagWithoutCategories(childNode);
            String originalContent = tag.getContent();
            String normalizedContent = replaceBoundaryWhitespace(originalContent);
            if (!originalContent.equals(normalizedContent)) {
                replacements.add(new ReferenceReplacement(originalContent, normalizedContent));
                changes.add(changeDescription(originalContent, normalizedContent));
            }
            if (!normalizedContent.equals(originalContent)
                && hasExactTarget(tagCategories, childNode, normalizedContent)) {
                tagCategories.removeNodeFromParent(childNode);
                continue;
            }
            if (!normalizedContent.equals(originalContent)) {
                DefaultTreeModel nodes = tagCategories.getNodes();
                TreePath pathToRoot = new TreePath(nodes.getPathToRoot(childNode));
                nodes.valueForPathChanged(pathToRoot, new Tag(normalizedContent, tag.getColor()));
            }
        }
    }

    private String replaceBoundaryWhitespaceInQualifiedName(String qualifiedName, String separator) {
        if (qualifiedName == null) {
            return "";
        }
        if (separator == null || separator.isEmpty()) {
            return replaceBoundaryWhitespace(qualifiedName);
        }
        String[] segments = qualifiedName.split(Pattern.quote(separator), -1);
        for (int i = 0; i < segments.length; i++) {
            segments[i] = replaceBoundaryWhitespace(segments[i]);
        }
        return String.join(separator, segments);
    }

    private String replaceBoundaryWhitespace(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        char[] chars = value.toCharArray();
        int leading = 0;
        while (leading < chars.length && chars[leading] <= ' ') {
            chars[leading] = '_';
            leading++;
        }
        for (int trailing = chars.length - 1; trailing >= 0 && chars[trailing] <= ' '; trailing--) {
            chars[trailing] = '_';
        }
        return new String(chars);
    }

    private boolean hasExactTarget(TagCategories tagCategories,
                                   DefaultMutableTreeNode currentNode,
                                   String normalizedContent) {
        DefaultMutableTreeNode uncategorizedNode = tagCategories.getUncategorizedTagsNode();
        for (int childIndex = 0; childIndex < uncategorizedNode.getChildCount(); childIndex++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) uncategorizedNode.getChildAt(childIndex);
            if (childNode != currentNode
                && tagCategories.tagWithoutCategories(childNode).getContent().equals(normalizedContent)) {
                return true;
            }
        }
        return hasExactCategorizedTarget(tagCategories, tagCategories.getRootNode(), normalizedContent);
    }

    private boolean hasExactCategorizedTarget(TagCategories tagCategories,
                                              DefaultMutableTreeNode parentNode,
                                              String normalizedContent) {
        for (DefaultMutableTreeNode childNode : childTagNodes(parentNode, tagCategories)) {
            if (tagCategories.categorizedContent(childNode).equals(normalizedContent)) {
                return true;
            }
            if (hasExactCategorizedTarget(tagCategories, childNode, normalizedContent)) {
                return true;
            }
        }
        return false;
    }

    private void applyReferenceRewrites(TagCategories tagCategories, List<ReferenceReplacement> replacements) {
        if (replacements.isEmpty()) {
            return;
        }
        ArrayList<String> replacementPairs = new ArrayList<>(replacements.size() * 2);
        replacements.stream()
            .sorted(Comparator.comparingInt((ReferenceReplacement replacement) -> replacement.from.length()).reversed())
            .forEach(replacement -> {
                replacementPairs.add(replacement.from);
                replacementPairs.add(replacement.to);
            });
        tagCategories.replaceReferencedTags(replacementPairs);
        tagCategories.updateTagReferences();
    }

    private List<DefaultMutableTreeNode> childTagNodes(DefaultMutableTreeNode parentNode, TagCategories tagCategories) {
        ArrayList<DefaultMutableTreeNode> childNodes = new ArrayList<>(parentNode.getChildCount());
        for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(childIndex);
            if (tagCategories.containsTag(childNode)) {
                childNodes.add(childNode);
            }
        }
        return childNodes;
    }

    private String changeDescription(String from, String to) {
        return translatedFormat("tag_category_repair_report_renamed", "\"{0}\" -> \"{1}\"", from, to);
    }

    private static String translatedText(String key, String fallback) {
        return Controller.getCurrentController() == null ? fallback : TextUtils.getText(key);
    }

    private static String translatedFormat(String key, String fallbackPattern, Object... arguments) {
        String pattern = Controller.getCurrentController() == null ? fallbackPattern : TextUtils.getText(key);
        return new MessageFormat(pattern).format(arguments);
    }
}
