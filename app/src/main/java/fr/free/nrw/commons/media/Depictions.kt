package fr.free.nrw.commons.media

import android.os.Parcelable
import androidx.annotation.WorkerThread
import fr.free.nrw.commons.wikidata.WikidataProperties.DEPICTS
import kotlinx.android.parcel.Parcelize
import org.wikipedia.wikidata.DataValue.EntityId
import org.wikipedia.wikidata.Entities

@Parcelize
data class Depictions(val depictions: List<IdAndLabel>) : Parcelable {
    companion object {
        @JvmStatic
        @WorkerThread
        fun from(entities: Entities, mediaClient: MediaClient) =
            Depictions(
                entities.first?.statements
                    ?.getOrElse(DEPICTS.propertyName, { emptyList() })
                    ?.map { statement ->
                        (statement.mainSnak.dataValue as EntityId).value.id
                    }
                    ?.map { id -> IdAndLabel(id, fetchLabel(mediaClient, id)) }
                    ?: emptyList()
            )

        private fun fetchLabel(mediaClient: MediaClient, id: String) =
            mediaClient.getLabelForDepiction(id).blockingGet()
    }
}
