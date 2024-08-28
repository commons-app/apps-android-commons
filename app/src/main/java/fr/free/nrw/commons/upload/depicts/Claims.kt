package fr.free.nrw.commons.upload.depicts

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.wikidata.model.Statement_partial

data class Claims(
    @SerializedName(value = "claims")
    val claims: Map<String, List<Statement_partial>> = emptyMap()
)