package org.freeplane.api;

/**
 * Supported edit operations for the map-level tag category API.
 * @since 1.13.3
 */
public enum MapTagCategoryInstructionType {
    /** Adds a category at the path given by the instruction. */
    ADD_CATEGORY,
    /** Adds a tag at the path given by the instruction. */
    ADD_TAG,
    /** Renames an existing category. */
    RENAME_CATEGORY,
    /** Renames an existing tag. */
    RENAME_TAG,
    /** Moves an existing category to another parent. */
    MOVE_CATEGORY,
    /** Moves an existing tag to another parent. */
    MOVE_TAG,
    /** Deletes an existing category. */
    DELETE_CATEGORY,
    /** Deletes an existing tag. */
    DELETE_TAG,
    /** Changes the color of an existing category or tag. */
    SET_COLOR,
    /** Changes the separator used in qualified category and tag names. */
    SET_CATEGORY_SEPARATOR
}
