package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TagCategoryEdit {
    private final TagCategoryEditType type;
    private final List<String> path;
    private final String newName;
    private final List<String> newParentPath;
    private final Integer index;
    private final String color;
    private final String newSeparator;

    public static TagCategoryEdit rename(List<String> path, String newName) {
        return new TagCategoryEdit(TagCategoryEditType.RENAME, path, newName, null, null, null, null);
    }

    public static TagCategoryEdit add(List<String> path) {
        return new TagCategoryEdit(TagCategoryEditType.ADD, path, null, null, null, null, null);
    }

    public static TagCategoryEdit delete(List<String> path) {
        return new TagCategoryEdit(TagCategoryEditType.DELETE, path, null, null, null, null, null);
    }

    public static TagCategoryEdit move(List<String> path, List<String> newParentPath, Integer index) {
        return new TagCategoryEdit(TagCategoryEditType.MOVE, path, null, newParentPath, index, null, null);
    }

    public static TagCategoryEdit setColor(List<String> path, String color) {
        return new TagCategoryEdit(TagCategoryEditType.SET_COLOR, path, null, null, null, color, null);
    }

    public static TagCategoryEdit setSeparator(String newSeparator) {
        return new TagCategoryEdit(TagCategoryEditType.SET_SEPARATOR, null, null, null, null, null, newSeparator);
    }

    public TagCategoryEdit(TagCategoryEditType type,
                           List<String> path,
                           String newName,
                           List<String> newParentPath,
                           Integer index,
                           String color,
                           String newSeparator) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.path = copyPath(path);
        this.newName = newName;
        this.newParentPath = copyPath(newParentPath);
        this.index = index;
        this.color = color;
        this.newSeparator = newSeparator;
        validate();
    }

    private List<String> copyPath(List<String> inputPath) {
        if (inputPath == null) {
            return null;
        }
        ArrayList<String> copy = new ArrayList<>(inputPath.size());
        for (String pathItem : inputPath) {
            if (isBlank(pathItem)) {
                throw new IllegalArgumentException("path contains blank segment");
            }
            copy.add(pathItem);
        }
        return Collections.unmodifiableList(copy);
    }

    private void validate() {
        switch (type) {
            case SET_SEPARATOR:
                if (isBlank(newSeparator)) {
                    throw new IllegalArgumentException("newSeparator must not be blank");
                }
                break;
            case RENAME:
                requirePath();
                if (isBlank(newName)) {
                    throw new IllegalArgumentException("newName must not be blank");
                }
                break;
            case MOVE:
                requirePath();
                if (newParentPath == null) {
                    throw new IllegalArgumentException("newParentPath must not be null");
                }
                break;
            case SET_COLOR:
                requirePath();
                if (isBlank(color)) {
                    throw new IllegalArgumentException("color must not be blank");
                }
                break;
            case ADD:
            case DELETE:
                requirePath();
                break;
            default:
                throw new IllegalArgumentException("Unsupported edit type: " + type);
        }
    }

    private void requirePath() {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public TagCategoryEditType getType() {
        return type;
    }

    public List<String> getPath() {
        return path;
    }

    public String getNewName() {
        return newName;
    }

    public List<String> getNewParentPath() {
        return newParentPath;
    }

    public Integer getIndex() {
        return index;
    }

    public String getColor() {
        return color;
    }

    public String getNewSeparator() {
        return newSeparator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, path, newName, newParentPath, index, color, newSeparator);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagCategoryEdit)) {
            return false;
        }
        TagCategoryEdit other = (TagCategoryEdit) obj;
        return type == other.type
            && Objects.equals(path, other.path)
            && Objects.equals(newName, other.newName)
            && Objects.equals(newParentPath, other.newParentPath)
            && Objects.equals(index, other.index)
            && Objects.equals(color, other.color)
            && Objects.equals(newSeparator, other.newSeparator);
    }
}
