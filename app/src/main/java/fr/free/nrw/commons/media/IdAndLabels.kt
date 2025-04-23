package fr.free.nrw.commons.media

data class IdAndLabels(
    val id: String,
    val labels: Map<String, String>,
) {
    // if a label is available in user's locale, return it
    // if not then check for english, else show any available.
    fun getLocalizedLabel(locale: String): String? {
        if (labels[locale] != null) {
            return labels[locale]
        }
        if (labels["en"] != null) {
            return labels["en"]
        }
        return labels.values.firstOrNull() ?: id
    }
}