package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TagCategoryInstruction {
    private final TagCategoryInstructionType type;
    private final List<String> path;
    private final String newName;
    private final List<String> newParentPath;
    private final Integer index;
    private final String color;
    private final String newSeparator;

    public static TagCategoryInstruction renameCategory(List<String> path, String newName) {
        return new TagCategoryInstruction(TagCategoryInstructionType.RENAME_CATEGORY,
            path, newName, null, null, null, null);
    }

    public static TagCategoryInstruction renameTag(List<String> path, String newName) {
        return new TagCategoryInstruction(TagCategoryInstructionType.RENAME_TAG,
            path, newName, null, null, null, null);
    }

    public static TagCategoryInstruction addCategory(List<String> path) {
        return new TagCategoryInstruction(TagCategoryInstructionType.ADD_CATEGORY,
            path, null, null, null, null, null);
    }

    public static TagCategoryInstruction addTag(List<String> path) {
        return new TagCategoryInstruction(TagCategoryInstructionType.ADD_TAG,
            path, null, null, null, null, null);
    }

    public static TagCategoryInstruction deleteCategory(List<String> path) {
        return new TagCategoryInstruction(TagCategoryInstructionType.DELETE_CATEGORY,
            path, null, null, null, null, null);
    }

    public static TagCategoryInstruction deleteTag(List<String> path) {
        return new TagCategoryInstruction(TagCategoryInstructionType.DELETE_TAG,
            path, null, null, null, null, null);
    }

    public static TagCategoryInstruction moveCategory(List<String> path, List<String> newParentPath, Integer index) {
        return new TagCategoryInstruction(TagCategoryInstructionType.MOVE_CATEGORY,
            path, null, newParentPath, index, null, null);
    }

    public static TagCategoryInstruction moveTag(List<String> path, List<String> newParentPath, Integer index) {
        return new TagCategoryInstruction(TagCategoryInstructionType.MOVE_TAG,
            path, null, newParentPath, index, null, null);
    }

    public static TagCategoryInstruction setColor(List<String> path, String color) {
        return new TagCategoryInstruction(TagCategoryInstructionType.SET_COLOR,
            path, null, null, null, color, null);
    }

    public static TagCategoryInstruction setCategorySeparator(String newSeparator) {
        return new TagCategoryInstruction(TagCategoryInstructionType.SET_CATEGORY_SEPARATOR,
            null, null, null, null, null, newSeparator);
    }

    public TagCategoryInstruction(TagCategoryInstructionType type,
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
            case SET_CATEGORY_SEPARATOR:
                if (isBlank(newSeparator)) {
                    throw new IllegalArgumentException("newSeparator must not be blank");
                }
                break;
            case RENAME_CATEGORY:
            case RENAME_TAG:
                requirePath();
                if (isBlank(newName)) {
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
                if (isBlank(color)) {
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public TagCategoryInstructionType getType() {
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
        if (!(obj instanceof TagCategoryInstruction)) {
            return false;
        }
        TagCategoryInstruction other = (TagCategoryInstruction) obj;
        return type == other.type
            && Objects.equals(path, other.path)
            && Objects.equals(newName, other.newName)
            && Objects.equals(newParentPath, other.newParentPath)
            && Objects.equals(index, other.index)
            && Objects.equals(color, other.color)
            && Objects.equals(newSeparator, other.newSeparator);
    }
}
