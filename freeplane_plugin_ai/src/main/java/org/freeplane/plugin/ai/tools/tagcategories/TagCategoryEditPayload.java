package org.freeplane.plugin.ai.tools.tagcategories;

import java.util.List;
import java.util.Objects;

import org.freeplane.features.icon.TagCategoryEdit;
import org.freeplane.features.icon.TagCategoryEditType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagCategoryEditPayload {
    private final TagCategoryEditType type;
    private final List<String> path;
    private final String newName;
    private final List<String> newParentPath;
    private final Integer index;
    private final String color;
    private final String newSeparator;

    @JsonCreator
    public TagCategoryEditPayload(@JsonProperty("type") TagCategoryEditType type,
                                  @JsonProperty("path") List<String> path,
                                  @JsonProperty("newName") String newName,
                                  @JsonProperty("newParentPath") List<String> newParentPath,
                                  @JsonProperty("index") Integer index,
                                  @JsonProperty("color") String color,
                                  @JsonProperty("newSeparator") String newSeparator) {
        this.type = type;
        this.path = path;
        this.newName = newName;
        this.newParentPath = newParentPath;
        this.index = index;
        this.color = color;
        this.newSeparator = newSeparator;
    }

    static TagCategoryEditPayload fromEdit(TagCategoryEdit edit) {
        return new TagCategoryEditPayload(
            edit.getType(),
            edit.getPath(),
            edit.getNewName(),
            edit.getNewParentPath(),
            edit.getIndex(),
            edit.getColor(),
            edit.getNewSeparator());
    }

    TagCategoryEdit toEdit() {
        return new TagCategoryEdit(type, path, newName, newParentPath, index, color, newSeparator);
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
        if (!(obj instanceof TagCategoryEditPayload)) {
            return false;
        }
        TagCategoryEditPayload other = (TagCategoryEditPayload) obj;
        return type == other.type
            && Objects.equals(path, other.path)
            && Objects.equals(newName, other.newName)
            && Objects.equals(newParentPath, other.newParentPath)
            && Objects.equals(index, other.index)
            && Objects.equals(color, other.color)
            && Objects.equals(newSeparator, other.newSeparator);
    }
}
