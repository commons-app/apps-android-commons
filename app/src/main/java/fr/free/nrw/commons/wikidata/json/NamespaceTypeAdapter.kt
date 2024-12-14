package fr.free.nrw.commons.wikidata.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import fr.free.nrw.commons.wikidata.model.page.Namespace
import java.io.IOException

class NamespaceTypeAdapter : TypeAdapter<Namespace>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, namespace: Namespace) {
        out.value(namespace.code().toLong())
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): Namespace {
        if (reader.peek() == JsonToken.STRING) {
            // Prior to 3210ce44, we marshaled Namespace as the name string of the enum, instead of
            // the code number. This introduces a backwards-compatible check for the string value.
            // TODO: remove after April 2017, when all older namespaces have been deserialized.
            return Namespace.valueOf(reader.nextString())
        }
        return Namespace.of(reader.nextInt())
    }
}
