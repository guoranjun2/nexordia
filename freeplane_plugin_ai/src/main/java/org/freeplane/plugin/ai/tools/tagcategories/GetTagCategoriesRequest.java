package org.freeplane.plugin.ai.tools.tagcategories;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class GetTagCategoriesRequest {
    @Description("Target map ID.")
    private final String mapIdentifier;

    @JsonCreator
    public GetTagCategoriesRequest(@JsonProperty("mapIdentifier") String mapIdentifier) {
        this.mapIdentifier = mapIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapIdentifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GetTagCategoriesRequest)) {
            return false;
        }
        GetTagCategoriesRequest other = (GetTagCategoriesRequest) obj;
        return Objects.equals(mapIdentifier, other.mapIdentifier);
    }
}
