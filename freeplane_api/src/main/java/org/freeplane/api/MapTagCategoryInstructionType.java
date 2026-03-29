package org.freeplane.api;

/**
 * Supported edit operations for the map-level tag category API.
 * @since 1.13.3
 */
public enum MapTagCategoryInstructionType {
    /** Adds a tag at the path given by the instruction. */
    ADD_TAG,
    /** Renames an existing tag. */
    RENAME_TAG,
    /** Moves an existing tag to another parent or into the uncategorized bucket. */
    MOVE_TAG,
    /** Deletes an existing tag. */
    DELETE_TAG,
    /** Changes the color of an existing tag. */
    SET_COLOR,
    /** Changes the separator used in qualified category and tag names. */
    SET_CATEGORY_SEPARATOR
}
