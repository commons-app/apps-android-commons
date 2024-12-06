package fr.free.nrw.commons.wikidata.json

import android.net.Uri
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException

class UriTypeAdapter : TypeAdapter<Uri>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Uri) {
        out.value(value.toString())
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): Uri {
        return Uri.parse(reader.nextString())
    }
}
