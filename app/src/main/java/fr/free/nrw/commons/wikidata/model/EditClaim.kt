package fr.free.nrw.commons.wikidata.model


data class EditClaim(val claims: List<Statement_partial>) {

    companion object {
        @JvmStatic
        fun from(entityIds: List<String>, propertyName: String): EditClaim {

            val list = mutableListOf<Statement_partial>()
            entityIds.forEach {
                list.add(
                    Statement_partial(
                        mainSnak = Snak_partial(
                            snakType = "value",
                            property = propertyName,
                            dataValue = DataValue.EntityId(
                                WikiBaseEntityValue(
                                    entityType = "item",
                                    id = it,
                                    numericId = it.removePrefix("Q").toLong()
                                )
                            )
                        ),
                        type = "statement",
                        rank = "preferred"
                    )
                )
            }
            return EditClaim(list)
        }
    }
}
