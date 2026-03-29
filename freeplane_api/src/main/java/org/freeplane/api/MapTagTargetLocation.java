package org.freeplane.api;

/**
 * Target placement for add and move operations in the map-level tag category API.
 * @since 1.13.3
 */
public enum MapTagTargetLocation {
    /** Place the tag in the categorized tree. */
    CATEGORIZED,
    /** Place the tag in the uncategorized bucket. */
    UNCATEGORIZED
}
