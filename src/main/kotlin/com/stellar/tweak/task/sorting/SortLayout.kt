package com.stellar.tweak.task.sorting

/**
 * Geometric orientation for laying out sorted inventory contents.
 */
enum class SortLayout {
    /**
     * Row-major placement (left-to-right, top-to-bottom) with dynamic row breaks between item groups.
     */
    ROW,

    /**
     * Column-major placement (top-to-bottom, left-to-right) with dynamic column breaks between item groups.
     */
    COLUMN,
    ;

    override fun toString(): String {
        return name.first() + name.substring(1).lowercase()
    }
}
