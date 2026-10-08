package fr.free.nrw.commons.wikidata.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import fr.free.nrw.commons.wikidata.model.page.Namespace
import java.io.IOException

class NamespaceTypeAdapter : TypeAdapter<Namespace>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, namespace: Namespace) {
        out.value(namespace.code().toLong())
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): Namespace = Namespace.of(reader.nextInt())
}
