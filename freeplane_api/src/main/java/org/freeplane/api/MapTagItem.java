package org.freeplane.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Tag item used by the map-level tag category API.
 * @since 1.13.3
 */
public class MapTagItem {
    private final List<String> path;
    private final String name;
    private final String qualifiedName;
    private final String color;

    /**
     * @param path full path of the tag item
     * @param name local tag name
     * @param qualifiedName qualified name built with the category separator
     * @param color tag color, or {@code null} if none is set
     */
    public MapTagItem(List<String> path, String name, String qualifiedName, String color) {
        this.path = copyPath(path);
        this.name = requireText(name, "name");
        this.qualifiedName = requireText(qualifiedName, "qualifiedName");
        this.color = color;
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

    /** Returns the full path of the tag item. */
    public List<String> getPath() {
        return path;
    }

    /** Returns the local tag name. */
    public String getName() {
        return name;
    }

    /** Returns the qualified name built with the category separator. */
    public String getQualifiedName() {
        return qualifiedName;
    }

    /** Returns the tag color, or {@code null} if none is set. */
    public String getColor() {
        return color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, qualifiedName, color);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapTagItem)) {
            return false;
        }
        MapTagItem other = (MapTagItem) obj;
        return Objects.equals(path, other.path)
            && Objects.equals(name, other.name)
            && Objects.equals(qualifiedName, other.qualifiedName)
            && Objects.equals(color, other.color);
    }
}
