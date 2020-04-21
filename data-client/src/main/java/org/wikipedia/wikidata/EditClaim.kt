package org.wikipedia.wikidata

import org.wikipedia.wikidata.DataValue.DataValueEntityId


data class EditClaim(val claims: List<Statement_partial>) {

    companion object {
        @JvmStatic
        fun from(entityId: String, propertyName: String) =
            EditClaim(
                listOf(
                    Statement_partial(
                        Snak_partial(
                            "value",
                            propertyName,
                            DataValueEntityId(
                                WikiBaseEntityValue(
                                    "item",
                                    entityId,
                                    entityId.removePrefix("Q").toLong()
                                )
                            )
                        ),
                        "statement",
                        "preferred"
                    )
                )
            )
    }
}
