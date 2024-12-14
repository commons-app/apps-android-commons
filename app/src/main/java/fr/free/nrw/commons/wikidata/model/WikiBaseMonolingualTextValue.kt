package fr.free.nrw.commons.wikidata.model

/*"value": {
  "type": "monolingualtext",
  "value": {
    "text": "some value",
    "language": "en"
  }
}*/

data class WikiBaseMonolingualTextValue(
    val text: String,
    val language: String,
)
