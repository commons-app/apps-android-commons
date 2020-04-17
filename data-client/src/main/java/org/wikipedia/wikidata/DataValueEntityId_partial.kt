package org.wikipedia.wikidata

import org.wikipedia.json.RuntimeTypeAdapterFactory
import org.wikipedia.wikidata.DataValue_partial.DataValueEntityId_partial
import org.wikipedia.wikidata.DataValue_partial.DataValueString_partial

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
    OR
    "datavalue": {
                "value": {
                  "latitude": 37.7733,
                  "longitude": -122.412255,
                  "altitude": null,
                  "precision": 1.0e-6,
                  "globe": "http://www.wikidata.org/entity/Q2"
                },
                "type": "globecoordinate"
              }
    OR
    "datavalue": {
                "value": {
                  "time": "+2019-12-03T00:00:00Z",
                  "timezone": 0,
                  "before": 0,
                  "after": 0,
                  "precision": 11,
                  "calendarmodel": "http://www.wikidata.org/entity/Q1985727"
                },
                "type": "time"
              }
    */
sealed class DataValue_partial(val type: String) {
    companion object {
        @JvmStatic
        val polymorphicTypeAdapter =
            RuntimeTypeAdapterFactory.of(
                DataValue_partial::class.java,
                DataValue_partial::type.name
            )
                .registerSubtype(
                    DataValueEntityId_partial::class.java,
                    DataValueEntityId_partial.TYPE
                )
                .registerSubtype(
                    DataValueString_partial::class.java,
                    DataValueString_partial.TYPE
                )
                .registerSubtype(
                    DataValueGloveCoordinate_partial::class.java,
                    DataValueGloveCoordinate_partial.TYPE
                )
                .registerSubtype(
                    DataValueTime_partial::class.java,
                    DataValueTime_partial.TYPE
                )
    }

    data class DataValueEntityId_partial(val value: WikiBaseEntityValue_partial) :
        DataValue_partial(TYPE) {
        companion object {
            const val TYPE = "wikibase-entityid"
        }
    }

    data class DataValueString_partial(val value: String) : DataValue_partial(TYPE) {
        companion object {
            const val TYPE = "string"
        }
    }

    class DataValueGloveCoordinate_partial() :
        DataValue_partial(TYPE) {
        companion object {
            const val TYPE = "globecoordinate"
        }
    }

    class DataValueTime_partial() : DataValue_partial(TYPE) {
        companion object {
            const val TYPE = "time"
        }
    }
}
