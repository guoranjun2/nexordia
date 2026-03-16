package org.freeplane.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Single explicit edit operation for the map's tag categories.
 * @since 1.13.3
 */
public class MapTagCategoryInstruction {
    private final MapTagCategoryInstructionType type;
    private final List<String> path;
    private final String newName;
    private final List<String> newParentPath;
    private final Integer index;
    private final String color;
    private final String newSeparator;

    /**
     * @param type operation kind
     * @param path target path; for add operations, the path of the item to create
     * @param newName replacement name for rename operations
     * @param newParentPath target parent path for move operations; an empty list means the top level
     * @param index optional insertion index for ordered add or move operations
     * @param color replacement color for color updates
     * @param newSeparator replacement category separator
     */
    public MapTagCategoryInstruction(MapTagCategoryInstructionType type,
                                     List<String> path,
                                     String newName,
                                     List<String> newParentPath,
                                     Integer index,
                                     String color,
                                     String newSeparator) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.path = copyPathOrNull(path);
        this.newName = newName;
        this.newParentPath = copyPathOrNull(newParentPath);
        this.index = index;
        this.color = color;
        this.newSeparator = newSeparator;
        validate();
    }

    private List<String> copyPathOrNull(List<String> sourcePath) {
        if (sourcePath == null) {
            return null;
        }
        ArrayList<String> copy = new ArrayList<>(sourcePath.size());
        for (String pathPart : sourcePath) {
            if (pathPart == null || pathPart.trim().isEmpty()) {
                throw new IllegalArgumentException("path segment must not be blank");
            }
            copy.add(pathPart);
        }
        return Collections.unmodifiableList(copy);
    }

    private void validate() {
        switch (type) {
            case SET_CATEGORY_SEPARATOR:
                if (newSeparator == null || newSeparator.trim().isEmpty()) {
                    throw new IllegalArgumentException("newSeparator must not be blank");
                }
                break;
            case RENAME_CATEGORY:
            case RENAME_TAG:
                requirePath();
                if (newName == null || newName.trim().isEmpty()) {
                    throw new IllegalArgumentException("newName must not be blank");
                }
                break;
            case MOVE_CATEGORY:
            case MOVE_TAG:
                requirePath();
                if (newParentPath == null) {
                    throw new IllegalArgumentException("newParentPath must not be null");
                }
                break;
            case SET_COLOR:
                requirePath();
                if (color == null || color.trim().isEmpty()) {
                    throw new IllegalArgumentException("color must not be blank");
                }
                break;
            case ADD_CATEGORY:
            case ADD_TAG:
            case DELETE_CATEGORY:
            case DELETE_TAG:
                requirePath();
                break;
            default:
                throw new IllegalArgumentException("Unsupported instruction type: " + type);
        }
    }

    private void requirePath() {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
    }

    /** Returns the operation kind. */
    public MapTagCategoryInstructionType getType() {
        return type;
    }

    /** Returns the target path. */
    public List<String> getPath() {
        return path;
    }

    /** Returns the replacement name for rename operations. */
    public String getNewName() {
        return newName;
    }

    /** Returns the target parent path for move operations. */
    public List<String> getNewParentPath() {
        return newParentPath;
    }

    /** Returns the optional insertion index for ordered add or move operations. */
    public Integer getIndex() {
        return index;
    }

    /** Returns the replacement color for color updates. */
    public String getColor() {
        return color;
    }

    /** Returns the replacement category separator. */
    public String getNewSeparator() {
        return newSeparator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, path, newName, newParentPath, index, color, newSeparator);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapTagCategoryInstruction)) {
            return false;
        }
        MapTagCategoryInstruction other = (MapTagCategoryInstruction) obj;
        return type == other.type
            && Objects.equals(path, other.path)
            && Objects.equals(newName, other.newName)
            && Objects.equals(newParentPath, other.newParentPath)
            && Objects.equals(index, other.index)
            && Objects.equals(color, other.color)
            && Objects.equals(newSeparator, other.newSeparator);
    }
}
