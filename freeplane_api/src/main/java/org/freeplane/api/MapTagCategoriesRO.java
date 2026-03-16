package org.freeplane.api;

/**
 * Read-only access to the map's tag category structure:
 * <code>map.tagCategories</code>.
 * @since 1.13.3
 */
public interface MapTagCategoriesRO {
	/** Returns the current map-level tag category structure. */
	MapTagCategoryState read();
}
