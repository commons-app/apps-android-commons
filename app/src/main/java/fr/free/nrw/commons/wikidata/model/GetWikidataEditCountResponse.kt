package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

class GetWikidataEditCountResponse(@field:SerializedName("edits") val wikidataEditCount: Int)