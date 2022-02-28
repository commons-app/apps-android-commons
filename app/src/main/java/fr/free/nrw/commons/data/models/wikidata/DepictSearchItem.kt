package fr.free.nrw.commons.data.models.wikidata

/**
 * Model class for Depiction item returned from API after calling searchForDepicts
"search": [
{
"repository": "local",
"id": "Q23444",
"concepturi": "http://www.wikidata.org/entity/Q23444",
"title": "Q23444",
"pageid": 26835,
"url": "//www.wikidata.org/wiki/Q23444",
"label": "white",
"description": "color",
"match": {
"type": "label",
"language": "en",
"text": "white"
}
}*/
class DepictSearchItem(
    val id: String,
    val pageid: String,
    val url: String,
    val label: String,
    val description: String?
)
