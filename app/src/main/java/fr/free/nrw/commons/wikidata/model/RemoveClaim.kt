package fr.free.nrw.commons.wikidata.model

data class RemoveClaim(val claims: List<ClaimRemoveRequest>) {
    companion object {
        @JvmStatic
        fun from(claimIds: List<String>): RemoveClaim {
            val claimsToRemove = mutableListOf<ClaimRemoveRequest>()

            claimIds.forEach {
                claimsToRemove.add(
                    ClaimRemoveRequest(id = it, remove = "")
                )
            }

            return RemoveClaim(claimsToRemove)
        }
    }
}

data class ClaimRemoveRequest(val id: String, val remove: String)