package org.freeplane.features.icon;

import org.freeplane.features.map.MapModel;

public interface TagCategoryAccess {
    TagCategorySnapshot readSnapshot(MapModel mapModel);

    TagCategorySnapshot applyEdits(MapModel mapModel, TagCategoryEditBatch editBatch);
}
