package fr.free.nrw.commons.nearby

import java.io.Serializable

/**
 * User choices for a Nearby Wikidata search.
 *
 * Defaults match an unfiltered map search. [toSparql] emits only the fragment
 * substituted for `${FILTERS}` in the map templates. Coordinates stay in those
 * templates and are never taken from these options.
 *
 * [labelNames] restores the place-type checks in the form. The query uses
 * [classQids], which the form fills from [Label.getQids].
 */
class NearbySearchOptions() : Serializable {
    var radiusKm: Double? = null
    var classQids: MutableList<String> = ArrayList()
    var labelNames: MutableList<String> = ArrayList()
    var includeSubclasses: Boolean = false
    var missingImageOnly: Boolean = false
    var excludeDestroyed: Boolean = false

    constructor(
        radiusKm: Double?,
        classQids: List<String>,
        labelNames: List<String>,
        includeSubclasses: Boolean,
        missingImageOnly: Boolean,
        excludeDestroyed: Boolean,
    ) : this() {
        this.radiusKm = radiusKm
        this.classQids = ArrayList(classQids)
        this.labelNames = ArrayList(labelNames)
        this.includeSubclasses = includeSubclasses
        this.missingImageOnly = missingImageOnly
        this.excludeDestroyed = excludeDestroyed
    }

    /**
     * Returns the SPARQL constraints for these options.
     *
     * An empty string leaves the map query unchanged. Q-ids must match `Q`
     * followed by digits; anything else is rejected so a stored value cannot
     * change the query structure.
     *
     * @return the `${FILTERS}` replacement, or an empty string when nothing is set
     * @throws IllegalArgumentException when a class id is not a Wikidata Q-id
     */
    fun toSparql(): String {
        val parts = ArrayList<String>()
        val qids = LinkedHashSet<String>()
        for (raw in classQids) {
            require(QID.matches(raw)) { "Invalid Wikidata id: $raw" }
            qids.add(raw)
        }
        if (qids.isNotEmpty()) {
            val path = if (includeSubclasses) "wdt:P31/wdt:P279?" else "wdt:P31"
            val values = qids.joinToString(" ") { "wd:$it" }
            parts.add("?item $path ?classFilter .\nVALUES ?classFilter { $values }")
        }
        if (missingImageOnly) {
            parts.add("FILTER NOT EXISTS { ?item wdt:P18 ?picFilter }")
        }
        if (excludeDestroyed) {
            parts.add("FILTER NOT EXISTS { ?item wdt:P576 ?destroyedFilter }")
            parts.add("FILTER NOT EXISTS { ?item wdt:P582 ?endTimeFilter }")
        }
        return parts.joinToString("\n")
    }

    companion object {
        private const val serialVersionUID = 1L

        const val PREF_KEY = "nearby_search_options"
        const val ARG_KEY = "options"

        val RADIUS_CHOICES_KM = listOf(1.0, 5.0, 10.0, 20.0, 50.0)

        /** SeekBar units between two labeled distances, including the map-view end. */
        const val SLIDER_SEGMENT = 100

        val RADIUS_SLIDER_MAX = RADIUS_CHOICES_KM.size * SLIDER_SEGMENT

        private const val KILOMETER_TENTHS = 10.0
        private const val SMALLEST_RADIUS_KM = 0.1

        private val QID = Regex("Q\\d+")

        /**
         * Converts a slider position into a search radius.
         *
         * Position 0 is the current map view. The labeled distances sit at equal
         * gaps, and positions between them interpolate in kilometers.
         *
         * @param progress seek position from 0 to [RADIUS_SLIDER_MAX]
         * @return radius in kilometers, or null for the current map view
         */
        fun radiusKmForSlider(progress: Int): Double? {
            if (progress <= 0) {
                return null
            }
            val segments = RADIUS_CHOICES_KM.size
            val position = progress.coerceIn(0, RADIUS_SLIDER_MAX).toDouble() / SLIDER_SEGMENT
            val index = if (position >= segments) segments - 1 else position.toInt()
            val fraction = if (position >= segments) 1.0 else position - index
            val start = if (index == 0) 0.0 else RADIUS_CHOICES_KM[index - 1]
            val end = RADIUS_CHOICES_KM[index]
            val kilometers = start + (end - start) * fraction
            val rounded = kotlin.math.round(kilometers * KILOMETER_TENTHS) / KILOMETER_TENTHS
            return if (rounded <= 0.0) SMALLEST_RADIUS_KM else rounded
        }

        /**
         * Converts a saved radius back into a slider position.
         *
         * @param radiusKm radius in kilometers, or null for the current map view
         * @return seek position from 0 to [RADIUS_SLIDER_MAX]
         */
        fun sliderProgressForRadius(radiusKm: Double?): Int {
            val choices = RADIUS_CHOICES_KM
            val progress =
                when {
                    radiusKm == null || radiusKm <= 0.0 -> 0
                    radiusKm >= choices.last() -> RADIUS_SLIDER_MAX
                    radiusKm < choices.first() -> {
                        val fraction = radiusKm / choices.first()
                        (fraction * SLIDER_SEGMENT).toInt().coerceAtLeast(1)
                    }
                    else -> {
                        var position = RADIUS_SLIDER_MAX
                        for (index in 0 until choices.size - 1) {
                            val start = choices[index]
                            val end = choices[index + 1]
                            if (radiusKm <= end) {
                                val fraction = (radiusKm - start) / (end - start)
                                position = (((index + 1) + fraction) * SLIDER_SEGMENT).toInt()
                                break
                            }
                        }
                        position
                    }
                }
            return progress
        }
    }
}
