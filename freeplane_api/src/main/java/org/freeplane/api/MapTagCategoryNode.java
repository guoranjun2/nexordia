package org.freeplane.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MapTagCategoryNode {
    private final MapTagCategoryNodeKind kind;
    private final List<String> path;
    private final String name;
    private final String qualifiedName;
    private final String color;
    private final List<MapTagCategoryNode> children;

    public MapTagCategoryNode(MapTagCategoryNodeKind kind,
                              List<String> path,
                              String name,
                              String qualifiedName,
                              String color,
                              List<MapTagCategoryNode> children) {
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
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

    public MapTagCategoryNodeKind getKind() {
        return kind;
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

    public List<MapTagCategoryNode> getChildren() {
        return children;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, path, name, qualifiedName, color, children);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapTagCategoryNode)) {
            return false;
        }
        MapTagCategoryNode other = (MapTagCategoryNode) obj;
        return kind == other.kind
            && Objects.equals(path, other.path)
            && Objects.equals(name, other.name)
            && Objects.equals(qualifiedName, other.qualifiedName)
            && Objects.equals(color, other.color)
            && Objects.equals(children, other.children);
    }
}
