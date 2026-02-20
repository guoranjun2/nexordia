package org.freeplane.features.icon;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.freeplane.core.util.ColorUtils;
import org.freeplane.features.map.MapModel;

public class TagCategorySnapshotBuilder {
    public static TagCategorySnapshot from(MapModel mapModel) {
        if (mapModel == null) {
            throw new IllegalArgumentException("mapModel must not be null");
        }
        return from(mapModel.getIconRegistry().getTagCategories());
    }

    public static TagCategorySnapshot from(TagCategories tagCategories) {
        if (tagCategories == null) {
            throw new IllegalArgumentException("tagCategories must not be null");
        }
        String separator = tagCategories.getTagCategorySeparator();
        List<TagCategoryNode> categories = buildCategories(tagCategories);
        List<TagDescriptor> uncategorizedTags = buildUncategorizedTags(tagCategories);
        String revision = revision(separator, categories, uncategorizedTags);
        return new TagCategorySnapshot(revision, separator, categories, uncategorizedTags);
    }

    private static List<TagCategoryNode> buildCategories(TagCategories tagCategories) {
        ArrayList<TagCategoryNode> categories = new ArrayList<>();
        DefaultMutableTreeNode rootNode = tagCategories.getRootNode();
        DefaultMutableTreeNode uncategorizedNode = tagCategories.getUncategorizedTagsNode();
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            if (childNode == uncategorizedNode || !tagCategories.containsTag(childNode)) {
                continue;
            }
            categories.add(buildNode(tagCategories, childNode, new ArrayList<>()));
        }
        return categories;
    }

    private static TagCategoryNode buildNode(TagCategories tagCategories,
                                             DefaultMutableTreeNode node,
                                             List<String> parentPath) {
        ArrayList<String> path = new ArrayList<>(parentPath);
        Tag categoryTag = tagCategories.tagWithoutCategories(node);
        path.add(categoryTag.getContent());
        ArrayList<TagCategoryNode> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            if (!tagCategories.containsTag(childNode)) {
                continue;
            }
            children.add(buildNode(tagCategories, childNode, path));
        }
        String qualifiedName = tagCategories.categorizedContent(node);
        return new TagCategoryNode(path, categoryTag.getContent(), qualifiedName,
            ColorUtils.colorToRGBAString(categoryTag.getColor()), children);
    }

    private static List<TagDescriptor> buildUncategorizedTags(TagCategories tagCategories) {
        ArrayList<TagDescriptor> uncategorizedTags = new ArrayList<>();
        for (Tag tag : tagCategories.getUncategorizedTags()) {
            ArrayList<String> path = new ArrayList<>(1);
            path.add(tag.getContent());
            uncategorizedTags.add(new TagDescriptor(path, tag.getContent(), tag.getContent(),
                ColorUtils.colorToRGBAString(tag.getColor())));
        }
        return uncategorizedTags;
    }

    private static String revision(String separator,
                                   List<TagCategoryNode> categories,
                                   List<TagDescriptor> uncategorizedTags) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            update(messageDigest, separator);
            appendCategories(messageDigest, categories);
            appendUncategorizedTags(messageDigest, uncategorizedTags);
            byte[] digest = messageDigest.digest();
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte byteValue : digest) {
                int unsigned = byteValue & 0xFF;
                if (unsigned < 16) {
                    hex.append('0');
                }
                hex.append(Integer.toHexString(unsigned));
            }
            return "sha256:" + hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendCategories(MessageDigest messageDigest, List<TagCategoryNode> categories) {
        update(messageDigest, Integer.toString(categories.size()));
        for (TagCategoryNode category : categories) {
            appendCategory(messageDigest, category);
        }
    }

    private static void appendCategory(MessageDigest messageDigest, TagCategoryNode category) {
        appendPath(messageDigest, category.getPath());
        update(messageDigest, category.getName());
        update(messageDigest, category.getQualifiedName());
        update(messageDigest, category.getColor());
        appendCategories(messageDigest, category.getChildren());
    }

    private static void appendUncategorizedTags(MessageDigest messageDigest, List<TagDescriptor> uncategorizedTags) {
        update(messageDigest, Integer.toString(uncategorizedTags.size()));
        for (TagDescriptor descriptor : uncategorizedTags) {
            appendPath(messageDigest, descriptor.getPath());
            update(messageDigest, descriptor.getName());
            update(messageDigest, descriptor.getQualifiedName());
            update(messageDigest, descriptor.getColor());
        }
    }

    private static void appendPath(MessageDigest messageDigest, List<String> path) {
        update(messageDigest, Integer.toString(path.size()));
        for (String pathPart : path) {
            update(messageDigest, pathPart);
        }
    }

    private static void update(MessageDigest messageDigest, String value) {
        String safeValue = value == null ? "<null>" : value;
        byte[] bytes = safeValue.getBytes(StandardCharsets.UTF_8);
        messageDigest.update((byte) 0x7C);
        messageDigest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.UTF_8));
        messageDigest.update((byte) 0x3A);
        messageDigest.update(bytes);
    }
}
