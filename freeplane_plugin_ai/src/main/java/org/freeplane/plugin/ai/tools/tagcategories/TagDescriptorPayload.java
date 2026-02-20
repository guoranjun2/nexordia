package org.freeplane.plugin.ai.tools.tagcategories;

import java.util.List;
import java.util.Objects;

import org.freeplane.features.icon.TagDescriptor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagDescriptorPayload {
    private final List<String> path;
    private final String name;
    private final String qualifiedName;
    private final String color;

    @JsonCreator
    public TagDescriptorPayload(@JsonProperty("path") List<String> path,
                                @JsonProperty("name") String name,
                                @JsonProperty("qualifiedName") String qualifiedName,
                                @JsonProperty("color") String color) {
        this.path = path;
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.color = color;
    }

    static TagDescriptorPayload fromDescriptor(TagDescriptor descriptor) {
        return new TagDescriptorPayload(
            descriptor.getPath(),
            descriptor.getName(),
            descriptor.getQualifiedName(),
            descriptor.getColor());
    }

    TagDescriptor toDescriptor() {
        return new TagDescriptor(path, name, qualifiedName, color);
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
        if (!(obj instanceof TagDescriptorPayload)) {
            return false;
        }
        TagDescriptorPayload other = (TagDescriptorPayload) obj;
        return Objects.equals(path, other.path)
            && Objects.equals(name, other.name)
            && Objects.equals(qualifiedName, other.qualifiedName)
            && Objects.equals(color, other.color);
    }
}
