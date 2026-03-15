package org.freeplane.features.icon;

import org.freeplane.features.map.MapModel;

public interface TagCategoryAccess {
    TagCategoryState readCurrentCategoryState(MapModel mapModel);

    TagCategoryState applyInstructionRequest(MapModel mapModel, TagCategoryInstructionRequest instructionRequest);

    TagCategoryState applyEditorDraftSubmission(MapModel mapModel, TagCategoryEditorDraftSubmission draftSubmission);
}
