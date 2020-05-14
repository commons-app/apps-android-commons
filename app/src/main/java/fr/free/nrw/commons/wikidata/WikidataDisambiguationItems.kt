package fr.free.nrw.commons.wikidata


enum class WikidataDisambiguationItems(val id: String) {
    DISAMBIGUATION_PAGE("Q4167410"), INTERNAL_ITEM("Q17442446"), CATEGORY("Q4167836");

    companion object {
        fun isDisambiguationItem(ids: List<String>) =
            values().any { disambiguationItem: WikidataDisambiguationItems ->
                ids.any { id -> disambiguationItem.id == id }
            }
    }
}
