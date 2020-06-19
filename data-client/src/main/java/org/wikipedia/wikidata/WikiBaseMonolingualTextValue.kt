package org.wikipedia.wikidata

import com.google.gson.annotations.SerializedName

/*"value": {
  "type": "monolingualtext",
  "value": {
    "text": "some value",
    "language": "en"
  }
}*/
data class WikiBaseMonolingualTextValue(
    val value: Value
)

data class Value(val text: String, val language: String)