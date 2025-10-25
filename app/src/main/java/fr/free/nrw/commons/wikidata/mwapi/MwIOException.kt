package fr.free.nrw.commons.wikidata.mwapi

import java.io.IOException

class MwIOException(string: String, val error: MwLegacyServiceError) : IOException(string)
