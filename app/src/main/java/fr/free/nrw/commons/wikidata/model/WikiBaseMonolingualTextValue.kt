package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

/*"value": {
  "type": "monolingualtext",
  "value": {
    "text": "some value",
    "language": "en"
  }
}*/

data class WikiBaseMonolingualTextValue(val text: String, val language: String)