package fr.free.nrw.commons.wikidata

import fr.free.nrw.commons.BuildConfig

enum class WikidataProperties(val propertyName: String) {
    IMAGE("P18"), DEPICTS(BuildConfig.DEPICTS_PROPERTY);

}
