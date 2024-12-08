package fr.free.nrw.commons.wikidata.mwapi

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.wikidata.json.PostProcessingTypeAdapter.PostProcessable
import fr.free.nrw.commons.wikidata.model.BaseModel

abstract class MwResponse : BaseModel(), PostProcessable {
    private val errors: List<MwServiceError>? = null

    @SerializedName("servedby")
    private val servedBy: String? = null

    override fun postProcess() {
        if (!errors.isNullOrEmpty()) {
            throw MwException(errors[0], errors)
        }
    }
}
