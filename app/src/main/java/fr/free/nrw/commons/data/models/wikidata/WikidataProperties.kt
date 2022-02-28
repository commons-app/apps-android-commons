package fr.free.nrw.commons.data.models.wikidata

import fr.free.nrw.commons.BuildConfig

enum class WikidataProperties(val propertyName: String) {
    IMAGE("P18"),
    DEPICTS(BuildConfig.DEPICTS_PROPERTY),
    COMMONS_CATEGORY("P373"),
    INSTANCE_OF("P31"),
    MEDIA_LEGENDS("P2096");
}
