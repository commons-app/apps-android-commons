package org.wikipedia.wikidata

import org.wikipedia.wikidata.DataValue.EntityId


data class EditClaim(val claims: List<Statement_partial>) {

    companion object {
        @JvmStatic
        fun from(entityIds: List<String>, propertyName: String): EditClaim {

            val list = mutableListOf<Statement_partial>()
            entityIds.forEach {
                list.add(
                    Statement_partial(
                        Snak_partial(
                            "value",
                            propertyName,
                            EntityId(
                                WikiBaseEntityValue(
                                    "item",
                                    it,
                                    it.removePrefix("Q").toLong()
                                )
                            )
                        ),
                        "statement",
                        "preferred"
                    )
                )
            }
            return EditClaim(list)
        }
    }
}
