package org.freeplane.plugin.script.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.TagCategoryAccess;
import org.freeplane.features.icon.TagCategoryEdit;
import org.freeplane.features.icon.TagCategoryEditBatch;
import org.freeplane.features.icon.TagCategoryEditType;
import org.freeplane.features.icon.TagCategoryNode;
import org.freeplane.features.icon.TagCategorySnapshot;
import org.freeplane.features.icon.TagDescriptor;
import org.freeplane.features.icon.mindmapmode.FreeplaneTagCategoryAccess;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.script.ScriptContext;

class MapTagCategoriesProxy extends AbstractProxy<MapModel> implements Proxy.MapTagCategories {
    private final TagCategoryAccess tagCategoryAccess;

    MapTagCategoriesProxy(MapModel delegate, ScriptContext scriptContext) {
        this(delegate, scriptContext, defaultAccess());
    }

    private static TagCategoryAccess defaultAccess() {
        return new FreeplaneTagCategoryAccess(
            (MIconController) MModeController.getMModeController().getExtension(IconController.class));
    }

    MapTagCategoriesProxy(MapModel delegate, ScriptContext scriptContext, TagCategoryAccess tagCategoryAccess) {
        super(delegate, scriptContext);
        this.tagCategoryAccess = Objects.requireNonNull(tagCategoryAccess, "tagCategoryAccess");
    }

    @Override
    public Map<String, Object> snapshot() {
        TagCategorySnapshot snapshot = tagCategoryAccess.readSnapshot(getDelegate());
        return toSnapshotMap(snapshot);
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> editBatch) {
        if (editBatch == null) {
            throw new IllegalArgumentException("Missing editBatch.");
        }
        String expectedRevision = requiredString(editBatch.get("expectedRevision"), "expectedRevision");
        List<TagCategoryEdit> operations = parseOperations(editBatch.get("operations"));
        TagCategoryEditBatch batch = new TagCategoryEditBatch(expectedRevision, operations);
        TagCategorySnapshot snapshot = tagCategoryAccess.applyEdits(getDelegate(), batch);
        return toSnapshotMap(snapshot);
    }

    private List<TagCategoryEdit> parseOperations(Object operationsSpec) {
        if (!(operationsSpec instanceof Collection)) {
            throw new IllegalArgumentException("Missing operations.");
        }
        ArrayList<TagCategoryEdit> operations = new ArrayList<>();
        int operationIndex = 0;
        for (Object operationSpec : (Collection<?>) operationsSpec) {
            if (!(operationSpec instanceof Map)) {
                throw new IllegalArgumentException("operations[" + operationIndex + "] must be a map.");
            }
            operations.add(parseOperation((Map<?, ?>) operationSpec, operationIndex));
            operationIndex++;
        }
        return operations;
    }

    private TagCategoryEdit parseOperation(Map<?, ?> operationSpec, int operationIndex) {
        TagCategoryEditType type = parseEditType(requiredString(operationSpec.get("type"),
            "operations[" + operationIndex + "].type"));
        List<String> path = stringList(operationSpec.get("path"), "operations[" + operationIndex + "].path", false);
        String newName = optionalString(operationSpec.get("newName"));
        List<String> newParentPath = stringList(operationSpec.get("newParentPath"),
            "operations[" + operationIndex + "].newParentPath", true);
        Integer index = optionalInteger(operationSpec.get("index"), "operations[" + operationIndex + "].index");
        String color = optionalString(operationSpec.get("color"));
        String newSeparator = optionalString(operationSpec.get("newSeparator"));
        return new TagCategoryEdit(type, path, newName, newParentPath, index, color, newSeparator);
    }

    private TagCategoryEditType parseEditType(String typeSpec) {
        try {
            return TagCategoryEditType.valueOf(typeSpec);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported type: " + typeSpec);
        }
    }

    private Integer optionalInteger(Object indexSpec, String fieldName) {
        if (indexSpec == null) {
            return null;
        }
        if (indexSpec instanceof Number) {
            return ((Number) indexSpec).intValue();
        }
        throw new IllegalArgumentException(fieldName + " must be a number.");
    }

    private List<String> stringList(Object pathSpec, String fieldName, boolean nullable) {
        if (pathSpec == null) {
            if (nullable) {
                return null;
            }
            return new ArrayList<>();
        }
        if (!(pathSpec instanceof Collection)) {
            throw new IllegalArgumentException(fieldName + " must be a list.");
        }
        ArrayList<String> path = new ArrayList<>();
        for (Object element : (Collection<?>) pathSpec) {
            if (!(element instanceof String)) {
                throw new IllegalArgumentException(fieldName + " must contain strings.");
            }
            String value = ((String) element).trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " must not contain blank values.");
            }
            path.add(value);
        }
        return path;
    }

    private String requiredString(Object value, String fieldName) {
        String result = optionalString(value);
        if (result == null) {
            throw new IllegalArgumentException("Missing " + fieldName + ".");
        }
        return result;
    }

    private String optionalString(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Expected string value.");
        }
        String stringValue = ((String) value).trim();
        return stringValue.isEmpty() ? null : stringValue;
    }

    private Map<String, Object> toSnapshotMap(TagCategorySnapshot snapshot) {
        LinkedHashMap<String, Object> snapshotMap = new LinkedHashMap<>();
        snapshotMap.put("revision", snapshot.getRevision());
        snapshotMap.put("separator", snapshot.getSeparator());
        snapshotMap.put("categories", toCategoryMaps(snapshot.getCategories()));
        snapshotMap.put("uncategorizedTags", toDescriptorMaps(snapshot.getUncategorizedTags()));
        return snapshotMap;
    }

    private List<Map<String, Object>> toCategoryMaps(List<TagCategoryNode> categories) {
        ArrayList<Map<String, Object>> categoryMaps = new ArrayList<>(categories.size());
        for (TagCategoryNode category : categories) {
            LinkedHashMap<String, Object> categoryMap = new LinkedHashMap<>();
            categoryMap.put("path", new ArrayList<>(category.getPath()));
            categoryMap.put("name", category.getName());
            categoryMap.put("qualifiedName", category.getQualifiedName());
            categoryMap.put("color", category.getColor());
            categoryMap.put("children", toCategoryMaps(category.getChildren()));
            categoryMaps.add(categoryMap);
        }
        return categoryMaps;
    }

    private List<Map<String, Object>> toDescriptorMaps(List<TagDescriptor> descriptors) {
        ArrayList<Map<String, Object>> descriptorMaps = new ArrayList<>(descriptors.size());
        for (TagDescriptor descriptor : descriptors) {
            LinkedHashMap<String, Object> descriptorMap = new LinkedHashMap<>();
            descriptorMap.put("path", new ArrayList<>(descriptor.getPath()));
            descriptorMap.put("name", descriptor.getName());
            descriptorMap.put("qualifiedName", descriptor.getQualifiedName());
            descriptorMap.put("color", descriptor.getColor());
            descriptorMaps.add(descriptorMap);
        }
        return descriptorMaps;
    }
}
