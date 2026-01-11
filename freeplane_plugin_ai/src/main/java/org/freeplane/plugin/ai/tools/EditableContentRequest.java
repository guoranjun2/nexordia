package org.freeplane.plugin.ai.tools;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EditableContentRequest {
    private final Set<EditableContentField> fields;
    private final Set<EditableContentRepresentation> representations;

    @JsonCreator
    public EditableContentRequest(@JsonProperty("fields") List<EditableContentField> fields,
                                  @JsonProperty("representations") List<EditableContentRepresentation> representations) {
        this.fields = fields == null ? Collections.emptySet() : EnumSet.copyOf(fields);
        this.representations = representations == null ? Collections.emptySet() : EnumSet.copyOf(representations);
    }

    public boolean includesField(EditableContentField field) {
        if (field == null) {
            return false;
        }
        return fields.isEmpty() || fields.contains(field);
    }

    public boolean includesRepresentation(EditableContentRepresentation representation) {
        if (representation == null) {
            return false;
        }
        return representations.isEmpty() || representations.contains(representation);
    }
}
