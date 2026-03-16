package org.freeplane.api;

/**
 * Read-write access to the map's tag category structure:
 * <code>map.tagCategories</code>.
 * @since 1.13.3
 */
public interface MapTagCategories extends MapTagCategoriesRO {
	/**
	 * Applies explicit tag category edits.
	 * Use the revision returned by {@link #read()} as the request base revision.
	 */
	MapTagCategoryState edit(MapTagCategoryInstructionRequest instructionRequest);
}
