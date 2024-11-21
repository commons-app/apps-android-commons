package fr.free.nrw.commons.utils

object MediaDataExtractorUtil {

    /**
     * Extracts a list of categories from | separated category string
     *
     * @param source
     * @return
     */
    @JvmStatic
    fun extractCategoriesFromList(source: String?): List<String> {
        if (source.isNullOrBlank()) {
            return emptyList()
        }
        val cats = source.split("|")
        val categories = mutableListOf<String>()
        for (category in cats) {
            if (category.trim().isNotBlank()) {
                categories.add(category)
            }
        }
        return categories
    }
}
