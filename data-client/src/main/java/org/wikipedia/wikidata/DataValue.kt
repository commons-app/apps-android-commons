package org.wikipedia.wikidata

import org.wikipedia.json.RuntimeTypeAdapterFactory

sealed class DataValue(val type: String) {
    companion object {
        @JvmStatic
        val polymorphicTypeAdapter =
            RuntimeTypeAdapterFactory.of(DataValue::class.java, DataValue::type.name)
                .registerSubtype(EntityId::class.java, EntityId.TYPE)
                .registerSubtype(ValueString::class.java, ValueString.TYPE)
                .registerSubtype(GlobeCoordinate_partial::class.java, GlobeCoordinate_partial.TYPE)
                .registerSubtype(Time_partial::class.java, Time_partial.TYPE)
                .registerSubtype(Quantity_partial::class.java, Quantity_partial.TYPE)
                .registerSubtype(MonoLingualText::class.java, MonoLingualText.TYPE)
    }

    //    "value": {
    //        "entity-type": "item",
    //        "id": "Q30",
    //        "numeric-id": 30
    //    },
    //    "type": "wikibase-entityid"
    //    }
    data class EntityId(val value: WikiBaseEntityValue) : DataValue(TYPE) {
        companion object {
            const val TYPE = "wikibase-entityid"
        }
    }

    //    {
    //        "value": "SomePicture.jpg",
    //        "type": "string"
    //    }
    data class ValueString(val value: String) : DataValue(TYPE) {
        companion object {
            const val TYPE = "string"
        }
    }

    //    "value": {
    //        "latitude": 37.7733,
    //        "longitude": -122.412255,
    //        "altitude": null,
    //        "precision": 1.0e-6,
    //        "globe": "http://www.wikidata.org/entity/Q2"
    //    },
    //    "type": "globecoordinate"
    //    }
    class GlobeCoordinate_partial() : DataValue(TYPE) {
        companion object {
            const val TYPE = "globecoordinate"
        }
    }

    //    "value": {
    //        "time": "+2019-12-03T00:00:00Z",
    //        "timezone": 0,
    //        "before": 0,
    //        "after": 0,
    //        "precision": 11,
    //        "calendarmodel": "http://www.wikidata.org/entity/Q1985727"
    //    },
    //    "type": "time"
    //    }
    class Time_partial() : DataValue(TYPE) {
        companion object {
            const val TYPE = "time"
        }
    }

    //    {
    //        "value": {
    //        "amount": "+587",
    //        "unit": "http://www.wikidata.org/entity/Q828224"
    //    }
    //    }
    class Quantity_partial() : DataValue(TYPE) {
        companion object {
            const val TYPE = "quantity"
        }
    }

    //    {
    //        "value": {
    //        "text": "활",
    //        "language": "ko"
    //    }
    //    }
    class MonoLingualText(val value: WikiBaseMonolingualTextValue) : DataValue(TYPE) {
        companion object {
            const val TYPE = "monolingualtext"
        }
    }
}
