package org.freeplane.features.icon;

import java.util.List;
import java.util.Objects;

public class TagItem {
    private final List<String> path;
    private final String name;
    private final String qualifiedName;
    private final String color;

    public TagItem(List<String> path, String name, String qualifiedName, String color) {
        this.path = copyPath(path);
        TagCategoryNamePolicy.requireNonBlank(name, "name");
        TagCategoryNamePolicy.requireNonBlank(qualifiedName, "qualifiedName");
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.color = color;
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

    @Override
    public int hashCode() {
        return Objects.hash(path, name, qualifiedName, color);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagItem)) {
            return false;
        }
        TagItem other = (TagItem) obj;
        return Objects.equals(path, other.path)
            && Objects.equals(name, other.name)
            && Objects.equals(qualifiedName, other.qualifiedName)
            && Objects.equals(color, other.color);
    }
}
