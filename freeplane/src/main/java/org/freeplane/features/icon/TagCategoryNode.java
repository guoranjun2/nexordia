package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TagCategoryNode {
    private final List<String> path;
    private final String name;
    private final String qualifiedName;
    private final String color;
    private final List<TagCategoryNode> children;

    public TagCategoryNode(List<String> path,
                           String name,
                           String qualifiedName,
                           String color,
                           List<TagCategoryNode> children) {
        this.path = copyPath(path);
        TagCategoryNamePolicy.requireNonBlank(name, "name");
        TagCategoryNamePolicy.requireNonBlank(qualifiedName, "qualifiedName");
        if (children == null) {
            throw new IllegalArgumentException("children must not be null");
        }
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.color = color;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    private List<String> copyPath(List<String> sourcePath) {
        return TagCategoryNamePolicy.copyValidatedPath(sourcePath);
    }

    public List<String> getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getColor() {
        return color;
    }

    public List<TagCategoryNode> getChildren() {
        return children;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, qualifiedName, color, children);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagCategoryNode)) {
            return false;
        }
        TagCategoryNode other = (TagCategoryNode) obj;
        return Objects.equals(path, other.path)
            && Objects.equals(name, other.name)
            && Objects.equals(qualifiedName, other.qualifiedName)
            && Objects.equals(color, other.color)
            && Objects.equals(children, other.children);
    }
}
