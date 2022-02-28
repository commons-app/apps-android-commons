package fr.free.nrw.commons.data.models.wikidata

import com.google.gson.annotations.SerializedName

class GetWikidataEditCountResponse(@field:SerializedName("edits") val wikidataEditCount: Int)