package org.wikipedia.wikidata

import org.wikipedia.json.RuntimeTypeAdapterFactory

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
sealed class DataValue(val type: String) {
    companion object {
        @JvmStatic
        val polymorphicTypeAdapter =
            RuntimeTypeAdapterFactory.of(DataValue::class.java, DataValue::type.name)
                .registerSubtype(DataValueEntityId::class.java, DataValueEntityId.TYPE)
                .registerSubtype(DataValueString::class.java, DataValueString.TYPE)
                .registerSubtype(
                    DataValueGlobeCoordinate_partial::class.java,
                    DataValueGlobeCoordinate_partial.TYPE
                )
                .registerSubtype(
                    DataValueTime_partial::class.java,
                    DataValueTime_partial.TYPE
                )
    }

    data class DataValueEntityId(val value: WikiBaseEntityValue) :
        DataValue(TYPE) {
        companion object {
            const val TYPE = "wikibase-entityid"
        }
    }

    data class DataValueString(val value: String) : DataValue(TYPE) {
        companion object {
            const val TYPE = "string"
        }
    }

    class DataValueGlobeCoordinate_partial() :
        DataValue(TYPE) {
        companion object {
            const val TYPE = "globecoordinate"
        }
    }

    class DataValueTime_partial() : DataValue(TYPE) {
        companion object {
            const val TYPE = "time"
        }
    }
}
