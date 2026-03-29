package org.freeplane.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A node in the map's tag category tree.
 * Categorized tag nodes may have zero or more children.
 * @since 1.13.3
 */
public class MapTagCategoryNode {
    private final List<String> path;
    private final String name;
    private final String qualifiedName;
    private final String color;
    private final List<MapTagCategoryNode> children;

    /**
     * @param path full path from the top-level categorized tag to this node
     * @param name local node name
     * @param qualifiedName qualified name built with the category separator
     * @param color node color, or {@code null} if none is set
     * @param children child categorized tags
     */
    public MapTagCategoryNode(List<String> path,
                              String name,
                              String qualifiedName,
                              String color,
                              List<MapTagCategoryNode> children) {
        this.path = copyPath(path);
        this.name = requireText(name, "name");
        this.qualifiedName = requireText(qualifiedName, "qualifiedName");
        if (children == null) {
            throw new IllegalArgumentException("children must not be null");
        }
        this.color = color;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    private List<String> copyPath(List<String> sourcePath) {
        if (sourcePath == null || sourcePath.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
        ArrayList<String> copy = new ArrayList<>(sourcePath.size());
        for (String pathPart : sourcePath) {
            copy.add(requireText(pathPart, "path"));
        }
        return Collections.unmodifiableList(copy);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    /** Returns the full path from the top-level categorized tag to this node. */
    public List<String> getPath() {
        return path;
    }

    /** Returns the local node name. */
    public String getName() {
        return name;
    }

    /** Returns the qualified name built with the category separator. */
    public String getQualifiedName() {
        return qualifiedName;
    }

    /** Returns the node color, or {@code null} if none is set. */
    public String getColor() {
        return color;
    }

    /** Returns child categorized tags. */
    public List<MapTagCategoryNode> getChildren() {
        return children;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, qualifiedName, color, children);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapTagCategoryNode)) {
            return false;
        }
        MapTagCategoryNode other = (MapTagCategoryNode) obj;
        return Objects.equals(path, other.path)
            && Objects.equals(name, other.name)
            && Objects.equals(qualifiedName, other.qualifiedName)
            && Objects.equals(color, other.color)
            && Objects.equals(children, other.children);
    }
}
