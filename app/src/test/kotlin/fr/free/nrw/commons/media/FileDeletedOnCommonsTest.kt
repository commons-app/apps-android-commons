package fr.free.nrw.commons.media

import com.google.gson.Gson
import fr.free.nrw.commons.wikidata.mwapi.MwIOException
import fr.free.nrw.commons.wikidata.mwapi.MwLegacyServiceError
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class FileDeletedOnCommonsTest {
    private val gson = Gson()

    @Test
    fun missingPageWithoutImageInfoIsADeletedFile() {
        val page =
            gson.fromJson(
                """{"title":"File:Gone.jpg","missing":true}""",
                MwQueryPage::class.java,
            )

        assertTrue(pagesIndicateDeletedFile(listOf(page)))
    }

    @Test
    fun emptyOrIncompletePagesAreNotADeletedFile() {
        val untitled = gson.fromJson("""{"title":"File:Here.jpg"}""", MwQueryPage::class.java)
        val withImage =
            gson.fromJson(
                """{"title":"File:Here.jpg","missing":true,"imageinfo":[{"size":1}]}""",
                MwQueryPage::class.java,
            )

        assertFalse(pagesIndicateDeletedFile(emptyList()))
        assertFalse(pagesIndicateDeletedFile(listOf(untitled)))
        assertFalse(pagesIndicateDeletedFile(listOf(withImage)))
    }

    @Test
    fun onlyADeletedFileErrorCounts() {
        val missingTitle =
            gson.fromJson(
                """{"code":"missingtitle","info":"The page you specified doesn't exist."}""",
                MwLegacyServiceError::class.java,
            )
        val badInteger =
            gson.fromJson(
                """{"code":"badinteger","info":"Invalid value"}""",
                MwLegacyServiceError::class.java,
            )

        assertTrue(FileDeletedOnCommonsException().isFileDeletedOnCommons())
        assertTrue(MwIOException("missing", missingTitle).isFileDeletedOnCommons())
        assertFalse(MwIOException("bad id", badInteger).isFileDeletedOnCommons())
        assertFalse(IOException("offline").isFileDeletedOnCommons())
    }
}
