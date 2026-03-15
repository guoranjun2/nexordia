package org.freeplane.plugin.script.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.freeplane.api.MapTagCategories;
import org.freeplane.api.MapTagCategoryInstruction;
import org.freeplane.api.MapTagCategoryInstructionRequest;
import org.freeplane.api.MapTagCategoryNode;
import org.freeplane.api.MapTagCategoryNodeKind;
import org.freeplane.api.MapTagCategoryState;
import org.freeplane.api.MapTagItem;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.TagCategoryAccess;
import org.freeplane.features.icon.TagCategoryInstruction;
import org.freeplane.features.icon.TagCategoryInstructionRequest;
import org.freeplane.features.icon.TagCategoryInstructionType;
import org.freeplane.features.icon.TagCategoryNode;
import org.freeplane.features.icon.TagCategoryState;
import org.freeplane.features.icon.TagItem;
import org.freeplane.features.icon.mindmapmode.FreeplaneTagCategoryAccess;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.script.ScriptContext;

class MapTagCategoriesProxy extends AbstractProxy<MapModel> implements Proxy.MapTagCategories, MapTagCategories {
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
    public MapTagCategoryState read() {
        return toApiState(tagCategoryAccess.readCurrentCategoryState(getDelegate()));
    }

    @Override
    public MapTagCategoryState edit(MapTagCategoryInstructionRequest instructionRequest) {
        if (instructionRequest == null) {
            throw new IllegalArgumentException("instructionRequest must not be null.");
        }
        TagCategoryState updatedState = tagCategoryAccess.applyInstructionRequest(
            getDelegate(),
            toCoreInstructionRequest(instructionRequest));
        return toApiState(updatedState);
    }

    private TagCategoryInstructionRequest toCoreInstructionRequest(MapTagCategoryInstructionRequest instructionRequest) {
        ArrayList<TagCategoryInstruction> instructions = new ArrayList<>(instructionRequest.getInstructions().size());
        for (MapTagCategoryInstruction instruction : instructionRequest.getInstructions()) {
            instructions.add(toCoreInstruction(instruction));
        }
        return new TagCategoryInstructionRequest(instructionRequest.getBaseRevision(), instructions);
    }

    private TagCategoryInstruction toCoreInstruction(MapTagCategoryInstruction instruction) {
        return new TagCategoryInstruction(
            TagCategoryInstructionType.valueOf(instruction.getType().name()),
            instruction.getPath(),
            instruction.getNewName(),
            instruction.getNewParentPath(),
            instruction.getIndex(),
            instruction.getColor(),
            instruction.getNewSeparator());
    }

    private MapTagCategoryState toApiState(TagCategoryState categoryState) {
        return new MapTagCategoryState(
            categoryState.getRevision(),
            categoryState.getCategorySeparator(),
            toApiNodes(categoryState.getCategories()),
            toApiItems(categoryState.getUncategorizedTags()));
    }

    private List<MapTagCategoryNode> toApiNodes(List<TagCategoryNode> categoryNodes) {
        ArrayList<MapTagCategoryNode> apiNodes = new ArrayList<>(categoryNodes.size());
        for (TagCategoryNode categoryNode : categoryNodes) {
            apiNodes.add(toApiNode(categoryNode));
        }
        return apiNodes;
    }

    private MapTagCategoryNode toApiNode(TagCategoryNode categoryNode) {
        MapTagCategoryNodeKind nodeKind = MapTagCategoryNodeKind.valueOf(categoryNode.getKind().name());
        return new MapTagCategoryNode(
            nodeKind,
            categoryNode.getPath(),
            categoryNode.getName(),
            categoryNode.getQualifiedName(),
            categoryNode.getColor(),
            toApiNodes(categoryNode.getChildren()));
    }

    private List<MapTagItem> toApiItems(List<TagItem> items) {
        ArrayList<MapTagItem> apiItems = new ArrayList<>(items.size());
        for (TagItem item : items) {
            apiItems.add(new MapTagItem(item.getPath(), item.getName(), item.getQualifiedName(), item.getColor()));
        }
        return apiItems;
    }
}
