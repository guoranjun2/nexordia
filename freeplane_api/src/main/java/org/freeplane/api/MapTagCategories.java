package org.freeplane.api;

import java.util.Map;

public interface MapTagCategories extends MapTagCategoriesRO {
	Map<String, Object> apply(Map<String, Object> editBatch);
}
