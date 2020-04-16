package org.wikipedia.wikidata

/*"datavalue": {
    "value": {
        "entity-type": "item",
        "id": "Q30",
        "numeric-id": 30
    },
    "type": "wikibase-entityid"
    }
    OR
    "datavalue": {
            "value": "SomePicture.jpg",
            "type": "string"
          }

    */
sealed class DataValue_partial(val type: String) {

    data class DataValueEntityId_partial(val value: WikiBaseEntityValue_partial) :
        DataValue_partial(TYPE) {
        companion object {
            const val TYPE = "wikibase-entityid"
        }
    }

    data class DataValueString_partial(val value: String) :
        DataValue_partial(TYPE) {
        companion object {
            const val TYPE = "string"
        }
    }
}
