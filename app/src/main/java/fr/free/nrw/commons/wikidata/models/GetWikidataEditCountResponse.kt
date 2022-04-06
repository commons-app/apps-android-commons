package fr.free.nrw.commons.wikidata.models

import com.google.gson.annotations.SerializedName

class GetWikidataEditCountResponse(@field:SerializedName("edits") val wikidataEditCount: Int)