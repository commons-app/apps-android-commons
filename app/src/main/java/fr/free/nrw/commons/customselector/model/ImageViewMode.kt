package fr.free.nrw.commons.customselector.model

private const val GRID_SPAN_COUNT = 3
private const val LIST_SPAN_COUNT = 1

/**
 * Layout modes supported by the custom selector image list.
 */
enum class ImageViewMode(
    val preferenceValue: String,
    val spanCount: Int,
) {
    GRID("grid", GRID_SPAN_COUNT),
    LIST("list", LIST_SPAN_COUNT),
    ;

    companion object {
        /**
         * Resolves a stored preference value, falling back to the grid layout.
         */
        fun from(value: String?): ImageViewMode = values().firstOrNull { it.preferenceValue == value } ?: GRID
    }
}
