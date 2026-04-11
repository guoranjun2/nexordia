package org.freeplane.features.icon;

import java.util.List;
import java.util.Objects;

public class TagCategoryInstruction {
    private final TagCategoryInstructionType type;
    private final List<String> path;
    private final String newName;
    private final List<String> newParentPath;
    private final TagTargetLocation targetLocation;
    private final Integer index;
    private final String color;
    private final String newSeparator;

    public static TagCategoryInstruction renameTag(List<String> path, String newName) {
        return new TagCategoryInstruction(TagCategoryInstructionType.RENAME_TAG,
            path, newName, null, null, null, null, null);
    }

    public static TagCategoryInstruction addTag(List<String> path, TagTargetLocation targetLocation) {
        return new TagCategoryInstruction(TagCategoryInstructionType.ADD_TAG,
            path, null, null, targetLocation, null, null, null);
    }

    public static TagCategoryInstruction deleteTag(List<String> path) {
        return new TagCategoryInstruction(TagCategoryInstructionType.DELETE_TAG,
            path, null, null, null, null, null, null);
    }

    public static TagCategoryInstruction moveTag(List<String> path,
                                                 TagTargetLocation targetLocation,
                                                 List<String> newParentPath,
                                                 Integer index) {
        return new TagCategoryInstruction(TagCategoryInstructionType.MOVE_TAG,
            path, null, newParentPath, targetLocation, index, null, null);
    }

    public static TagCategoryInstruction setColor(List<String> path, String color) {
        return new TagCategoryInstruction(TagCategoryInstructionType.SET_COLOR,
            path, null, null, null, null, color, null);
    }

    public static TagCategoryInstruction setCategorySeparator(String newSeparator) {
        return new TagCategoryInstruction(TagCategoryInstructionType.SET_CATEGORY_SEPARATOR,
            null, null, null, null, null, null, newSeparator);
    }

    public TagCategoryInstruction(TagCategoryInstructionType type,
                                  List<String> path,
                                  String newName,
                                  List<String> newParentPath,
                                  TagTargetLocation targetLocation,
                                  Integer index,
                                  String color,
                                  String newSeparator) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.path = copyPath(path);
        this.newName = newName;
        this.newParentPath = copyPath(newParentPath);
        this.targetLocation = targetLocation;
        this.index = index;
        this.color = color;
        this.newSeparator = newSeparator;
        validate();
    }

    private List<String> copyPath(List<String> inputPath) {
        return TagCategoryNamePolicy.copyValidatedOptionalPath(inputPath);
    }

    private void validate() {
        switch (type) {
            case SET_CATEGORY_SEPARATOR:
                TagCategoryNamePolicy.requireNonBlank(newSeparator, "newSeparator");
                break;
            case RENAME_TAG:
                requirePath();
                TagCategoryNamePolicy.requireNonBlank(newName, "newName");
                break;
            case MOVE_TAG:
                requirePath();
                requireTargetLocation();
                validateMoveTarget();
                break;
            case SET_COLOR:
                requirePath();
                TagCategoryNamePolicy.requireNonBlank(color, "color");
                break;
            case ADD_TAG:
                requirePath();
                requireTargetLocation();
                validateAddTarget();
                break;
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

    private void requireTargetLocation() {
        if (targetLocation == null) {
            throw new IllegalArgumentException("targetLocation must not be null");
        }
    }

    private void validateAddTarget() {
        if (targetLocation == TagTargetLocation.UNCATEGORIZED && path.size() != 1) {
            throw new IllegalArgumentException("uncategorized tag path must contain exactly one segment");
        }
    }

    private void validateMoveTarget() {
        if (targetLocation == TagTargetLocation.CATEGORIZED) {
            if (newParentPath == null) {
                throw new IllegalArgumentException("newParentPath must not be null for categorized move");
            }
            return;
        }
        if (newParentPath != null && !newParentPath.isEmpty()) {
            throw new IllegalArgumentException("newParentPath must be omitted or empty for uncategorized move");
        }
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

    public TagTargetLocation getTargetLocation() {
        return targetLocation;
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
        return Objects.hash(type, path, newName, newParentPath, targetLocation, index, color, newSeparator);
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
            && targetLocation == other.targetLocation
            && Objects.equals(index, other.index)
            && Objects.equals(color, other.color)
            && Objects.equals(newSeparator, other.newSeparator);
    }
}
