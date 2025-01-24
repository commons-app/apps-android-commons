package fr.free.nrw.commons.wikidata.model

data class EditClaim(
    val claims: List<StatementPartial>,
) {
    companion object {
        @JvmStatic
        fun from(
            entityIds: List<String>,
            propertyName: String,
        ): EditClaim {
            val list = mutableListOf<StatementPartial>()
            entityIds.forEach {
                list.add(
                    StatementPartial(
                        mainSnak =
                            SnakPartial(
                                snakType = "value",
                                property = propertyName,
                                dataValue =
                                    DataValue.EntityId(
                                        WikiBaseEntityValue(
                                            entityType = "item",
                                            id = it,
                                            numericId = it.removePrefix("Q").toLong(),
                                        ),
                                    ),
                            ),
                        type = "statement",
                        rank = "preferred",
                    ),
                )
            }
            return EditClaim(list)
        }
    }
}
