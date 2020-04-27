package fr.free.nrw.commons.media

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wikipedia.wikidata.Entities

@Parcelize
data class IdAndLabel(val entityId: String, val entityLabel: String) : Parcelable {
    constructor(entityId: String, entities: MutableMap<String, Entities.Entity>) : this(
        entityId,
        entities.values.first().labels().values.first().value()
    )
}

