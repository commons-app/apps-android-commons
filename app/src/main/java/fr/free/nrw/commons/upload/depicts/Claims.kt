package fr.free.nrw.commons.upload.depicts

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.wikidata.model.StatementPartial

data class Claims(
    @SerializedName(value = "claims")
    val claims: Map<String, List<StatementPartial>> = emptyMap(),
)
