package fr.free.nrw.commons.wikidata.json

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import fr.free.nrw.commons.wikidata.model.WikiSite
import java.io.IOException

class WikiSiteTypeAdapter : TypeAdapter<WikiSite>() {
    @Throws(IOException::class)
    override fun write(
        out: JsonWriter,
        value: WikiSite,
    ) {
        out.beginObject()
        out.name(DOMAIN)
        out.value(value.url())

        out.name(LANGUAGE_CODE)
        out.value(value.languageCode())
        out.endObject()
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): WikiSite {
        var domain: String? = null
        var languageCode: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            val field = reader.nextName()
            val value = reader.nextString()
            when (field) {
                DOMAIN -> domain = value
                LANGUAGE_CODE -> languageCode = value
                else -> {}
            }
        }
        reader.endObject()

        if (domain == null) {
            throw JsonParseException("Missing domain")
        }

        return WikiSite(domain, languageCode ?: throw JsonParseException("Missing languageCode"))
    }

    companion object {
        private const val DOMAIN = "domain"
        private const val LANGUAGE_CODE = "languageCode"
    }
}
