package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

data class Claim(@SerializedName("type")
                           var type: String? = null,
                 @SerializedName("mainsnak")
                           var mainsnak: SimpleSnak? = null,
                 @SerializedName("id")
                           var id: String? = null,
                 @SerializedName("references")
                           var references: List<Reference>? = null,
                 @SerializedName("rank")
                           var rank: String? = null)