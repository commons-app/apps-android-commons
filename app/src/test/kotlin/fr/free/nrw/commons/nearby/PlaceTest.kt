package fr.free.nrw.commons.nearby

import com.google.gson.Gson
import fr.free.nrw.commons.nearby.model.NearbyResultItem
import fr.free.nrw.commons.upload.UploadMediaDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaceTest {
    // Bindings below are shaped like real WDQS answers for radius_query_for_upload_wizard.rq
    private fun placeFrom(binding: String): Place = Place.from(Gson().fromJson(binding, NearbyResultItem::class.java))

    @Test
    fun descriptionInSameLanguageIsKept() {
        val place =
            placeFrom(
                """
                {"item": {"type": "uri", "value": "http://www.wikidata.org/entity/Q90"},
                 "location": {"type": "literal", "value": "Point(2.35 48.85)"},
                 "label": {"xml:lang": "he", "type": "literal", "value": "פריז"},
                 "description": {"xml:lang": "he", "type": "literal", "value": "בירת צרפת"}}
                """,
            )
        assertEquals("he", place.language)
        assertEquals("פריז (בירת צרפת)", place.longDescription)
    }

    @Test
    fun descriptionInOtherLanguageIsDropped() {
        val place =
            placeFrom(
                """
                {"item": {"type": "uri", "value": "http://www.wikidata.org/entity/Q56696204"},
                 "location": {"type": "literal", "value": "Point(34.78 32.08)"},
                 "label": {"xml:lang": "he", "type": "literal", "value": "שדה פרפרים"},
                 "description": {"xml:lang": "en", "type": "literal", "value": "sculpture by Dudu Gerstein"}}
                """,
            )
        assertEquals("he", place.language)
        assertEquals("שדה פרפרים", place.longDescription)
    }

    @Test
    fun labelWithoutLanguageHasNoLanguage() {
        // WDQS returns the bare QID without xml:lang when no fallback language has a label
        val place =
            placeFrom(
                """
                {"item": {"type": "uri", "value": "http://www.wikidata.org/entity/Q131354436"},
                 "location": {"type": "literal", "value": "Point(34.78 32.08)"},
                 "label": {"type": "literal", "value": "Q131354436"}}
                """,
            )
        assertEquals("", place.language)
        assertNull(UploadMediaDetail(place).languageCode)
    }
}
