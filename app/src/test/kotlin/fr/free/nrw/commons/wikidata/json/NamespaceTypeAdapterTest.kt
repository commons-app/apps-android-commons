package fr.free.nrw.commons.wikidata.json

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import fr.free.nrw.commons.wikidata.model.page.Namespace
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter

class NamespaceTypeAdapterTest {
    private val adapter = NamespaceTypeAdapter()

    @Test
    fun `reads integer namespace code`() {
        val reader = JsonReader(StringReader("6"))
        assertEquals(Namespace.FILE, adapter.read(reader))
    }

    @Test
    fun `writes namespace as integer code`() {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw), Namespace.FILE)
        assertEquals("6", sw.toString())
    }
}
