package fr.free.nrw.commons.wikidata.json

import android.net.Uri
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import fr.free.nrw.commons.wikidata.model.WikiSite
import java.io.IOException

class WikiSiteTypeAdapter : TypeAdapter<WikiSite>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: WikiSite) {
        out.beginObject()
        out.name(DOMAIN)
        out.value(value.url())

        out.name(LANGUAGE_CODE)
        out.value(value.languageCode())
        out.endObject()
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): WikiSite {
        // todo: legacy; remove reader June 2018
        if (reader.peek() == JsonToken.STRING) {
            return WikiSite(Uri.parse(reader.nextString()))
        }

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

        // todo: legacy; remove reader June 2018
        return if (languageCode == null) {
            WikiSite(domain)
        } else {
            WikiSite(domain, languageCode)
        }
    }

    companion object {
        private const val DOMAIN = "domain"
        private const val LANGUAGE_CODE = "languageCode"
    }
}
