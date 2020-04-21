package org.wikipedia.wikidata


data class ClaimsResponse(val claims: Map<String, List<Statement_partial>>)
